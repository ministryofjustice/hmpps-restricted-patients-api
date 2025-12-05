package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

class RestrictedPatientIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun resetMocks() {
    prisonApiMockServer.resetAll()
    prisonerSearchApiMockServer.resetAll()
  }

  @Nested
  @DisplayName("/discharge-to-hospital")
  inner class DischargeToHospital {
    @Test
    fun `discharge a prisoner to hospital`() {
      dischargePrisonerWebClient(prisonerNumber = "A12345")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.prisonerNumber").isEqualTo("A12345")
        .jsonPath("$.fromLocation.agencyId").isEqualTo("MDI")
        .jsonPath("$.hospitalLocation.agencyId").isEqualTo("HAZLWD")
        .jsonPath("$.supportingPrison.agencyId").isEqualTo("MDI")
        .jsonPath("$.dischargeTime").isEqualTo("2020-10-10T00:00:00")
        .jsonPath("$.commentText").isEqualTo("Prisoner was released to hospital")

      prisonApiMockServer.verify(
        putRequestedFor(urlEqualTo("/api/offenders/A12345/discharge-to-hospital"))
          .withRequestBody(
            equalToJson(loadResourceFile("discharge-to-hospital-request.json")),
          ).withHeader("Authorization", WireMock.containing("Bearer")),
      )

      prisonApiMockServer.verify(
        getRequestedFor(urlPathEqualTo("/api/bookings/offenderNo/A12345"))
          .withQueryParam("fullInfo", equalTo("true"))
          .withQueryParam("extraInfo", equalTo("false"))
          .withQueryParam("csraSummary", equalTo("false"))
          .withHeader("Authorization", WireMock.containing("Bearer")),
      )

      verify(domainEventPublisher).publishRestrictedPatientAdded("A12345")

      verify(telemetryClient).trackEvent(
        "restricted-patient-added-discharge",
        mapOf(
          "prisonerNumber" to "A12345",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI",
          "dischargeTime" to "2020-10-10T00:00",
        ),
        null,
      )
    }

    @Nested
    inner class DischargeToHospitalErrors {
      @Test
      fun `should error if offender is not in prison`() {
        dischargePrisonerWebClient(prisonerNumber = "A12345", activeFlag = false)
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("$.status").isEqualTo(404)
      }

      @Test
      @WithMockAuthUser
      fun `should error if offender is already a restricted patient`() {
        saveRestrictedPatient(prisonerNumber = "A12345")
        dischargePrisonerWebClient(prisonerNumber = "A12345")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.status").isEqualTo(400)
          .jsonPath("$.errorCode").isEqualTo("EXISTING_PATIENT")
      }

      @Test
      fun `should error if prison-api errors`() {
        dischargePrisonerWebClientErrors("A12345")
          .exchange()
          .expectStatus().is5xxServerError
          .expectBody()
          .jsonPath("$.status").isEqualTo(500)
          .jsonPath("$.errorCode").isEqualTo("UPSTREAM_ERROR")

        assertThat(restrictedPatientRepository.findById("A12345")).isEmpty
      }

      @Test
      fun `should commit service transaction before publishing event`() {
        assertThat(restrictedPatientRepository.findById("A12345")).isNotPresent

        doThrow(RuntimeException()).whenever(domainEventPublisher).publishRestrictedPatientAdded(any())

        dischargePrisonerWebClient(prisonerNumber = "A12345")
          .exchange()
          .expectStatus().is5xxServerError

        assertThat(restrictedPatientRepository.findById("A12345")).isPresent

        verify(telemetryClient).trackEvent(
          "restricted-patient-added-discharge",
          mapOf(
            "prisonerNumber" to "A12345",
            "fromLocationId" to "MDI",
            "hospitalLocationCode" to "HAZLWD",
            "supportingPrisonId" to "MDI",
            "dischargeTime" to "2020-10-10T00:00",
          ),
          null,
        )
      }
    }
  }

  @Nested
  @DisplayName("/migrate-in-restricted-patient")
  inner class MigrateInPatient {
    @Test
    fun `migrate in a patient that has been moved out using NOMIS only`() {
      assertThat(restrictedPatientRepository.findById("A12345")).isNotPresent

      migrateInRestrictedPatientWebClient(prisonerNumber = "A12345")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.prisonerNumber").isEqualTo("A12345")
        .jsonPath("$.fromLocation.agencyId").isEqualTo("MDI")
        .jsonPath("$.hospitalLocation.agencyId").isEqualTo("HAZLWD")
        .jsonPath("$.supportingPrison.agencyId").isEqualTo("MDI")
        .jsonPath("$.dischargeTime").isEqualTo("2022-05-20T14:36:13")
        .jsonPath("$.commentText").isEqualTo("Historical discharge to hospital added to restricted patients")

      prisonApiMockServer.verify(
        postRequestedFor(urlEqualTo("/api/movements/offenders?latestOnly=true&allBookings=false"))
          .withRequestBody(
            equalToJson("""["A12345"]"""),
          ).withHeader("Authorization", WireMock.containing("Bearer")),
      )

      assertThat(restrictedPatientRepository.findById("A12345")).isPresent

      verify(domainEventPublisher).publishRestrictedPatientAdded("A12345")

      verify(telemetryClient).trackEvent(
        "restricted-patient-added-migrate",
        mapOf(
          "prisonerNumber" to "A12345",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI",
          "dischargeTime" to "2022-05-20T14:36:13",
        ),
        null,
      )
    }

    @Nested
    inner class MigrateInPatientErrors {

      @Test
      @WithMockAuthUser
      fun `should error if offender is already a restricted patient`() {
        saveRestrictedPatient(prisonerNumber = "A12345")
        migrateInRestrictedPatientWebClient(prisonerNumber = "A12345")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.status").isEqualTo(400)
          .jsonPath("$.errorCode").isEqualTo("EXISTING_PATIENT")
      }

      @Test
      fun `should error if offender is not released`() {
        migrateInRestrictedPatientWebClientNotReleased(prisonerNumber = "A12345")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.status").isEqualTo(400)
          .jsonPath("$.errorCode").isEqualTo("LAST_MOVE_NOT_REL")

        assertThat(restrictedPatientRepository.findById("A12345")).isEmpty
      }

      @Test
      fun `should error if prison-api errors`() {
        migrateInRestrictedPatientWebClientError("A12345")
          .exchange()
          .expectStatus().is5xxServerError
          .expectBody()
          .jsonPath("$.status").isEqualTo(500)
          .jsonPath("$.errorCode").isEqualTo("UPSTREAM_ERROR")

        assertThat(restrictedPatientRepository.findById("A12345")).isEmpty
      }

      @Test
      fun `should commit service transaction before publishing event`() {
        assertThat(restrictedPatientRepository.findById("A12345")).isNotPresent

        doThrow(RuntimeException()).whenever(domainEventPublisher).publishRestrictedPatientAdded(any())

        migrateInRestrictedPatientWebClient(prisonerNumber = "A12345")
          .exchange()
          .expectStatus().is5xxServerError

        assertThat(restrictedPatientRepository.findById("A12345")).isPresent

        verify(telemetryClient).trackEvent(
          "restricted-patient-added-migrate",
          mapOf(
            "prisonerNumber" to "A12345",
            "fromLocationId" to "MDI",
            "hospitalLocationCode" to "HAZLWD",
            "supportingPrisonId" to "MDI",
            "dischargeTime" to "2022-05-20T14:36:13",
          ),
          null,
        )
      }
    }
  }

  @Nested
  @DisplayName("/restricted-patient/prison-number/{prison-number}")
  inner class GetRestrictedPatient {
    @Test
    fun `returns restricted patient by prisoner number`() {
      dischargePrisonerWebClient(prisonerNumber = "A16345")
        .exchange()
        .expectStatus().isCreated

      getRestrictedPatient("A16345")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()
        .json(loadResourceFile("restricted-patient-by-prison-number-response.json"))
    }
  }

  @Nested
  @DisplayName("/restricted-patient/prison-number/{prison-number}")
  inner class RemoveRestrictedPatient {
    @Test
    fun `remove restricted patient`() {
      prisonApiMockServer.stubCreateExternalMovement()
      prisonerSearchApiMockServer.stubSearchByPrisonNumber("A12345")

      dischargePrisonerWebClient(prisonerNumber = "A12345")
        .exchange()
        .expectStatus().isCreated

      getRestrictedPatient(prisonerNumber = "A12345")
        .exchange()
        .expectStatus().is2xxSuccessful

      webTestClient.delete().uri("/restricted-patient/prison-number/A12345")
        .headers(setHeaders())
        .exchange()
        .expectStatus().is2xxSuccessful

      prisonApiMockServer.verify(
        postRequestedFor(urlEqualTo("/api/movements"))
          .withRequestBody(
            equalToJson(loadResourceFile("create-external-movement-request.json")),
          ),
      )

      getRestrictedPatient(prisonerNumber = "A12345")
        .exchange()
        .expectStatus().isNotFound

      verify(domainEventPublisher).publishRestrictedPatientRemoved("A12345")

      verify(telemetryClient).trackEvent(
        "restricted-patient-removed",
        mapOf(
          "prisonerNumber" to "A12345",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI",
          "dischargeTime" to "2020-10-10T00:00",
        ),
        null,
      )
    }

    @Nested
    inner class RemovePatientErrors {
      @Test
      fun `should error if not a restricted patient`() {
        webTestClient.delete().uri("/restricted-patient/prison-number/A12345")
          .headers(setHeaders())
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      @WithMockAuthUser
      fun `should error if prison-api errors`() {
        saveRestrictedPatient(prisonerNumber = "A12345")
        prisonerSearchApiMockServer.stubSearchByPrisonNumber("A12345")
        prisonApiMockServer.stubServerError(WireMock::post)

        webTestClient.delete().uri("/restricted-patient/prison-number/A12345")
          .headers(setHeaders())
          .exchange()
          .expectStatus().is5xxServerError
          .expectBody()
          .jsonPath("$.status").isEqualTo(500)
          .jsonPath("$.errorCode").isEqualTo("UPSTREAM_ERROR")

        assertThat(restrictedPatientRepository.findById("A12345")).isNotEmpty
      }

      @Test
      @WithMockAuthUser
      fun `should error if prisoner search api errors`() {
        saveRestrictedPatient(prisonerNumber = "A12345")

        webTestClient.delete().uri("/restricted-patient/prison-number/A12345")
          .headers(setHeaders())
          .exchange()
          .expectStatus().is5xxServerError
          .expectBody()
          .jsonPath("$.status").isEqualTo(500)
          .jsonPath("$.errorCode").isEqualTo("UPSTREAM_ERROR")

        assertThat(restrictedPatientRepository.findById("A12345")).isNotEmpty
      }

      @Test
      @WithMockAuthUser
      fun `should commit service transaction before publishing event`() {
        saveRestrictedPatient(prisonerNumber = "A12345")
        prisonerSearchApiMockServer.stubSearchByPrisonNumber("A12345")
        prisonApiMockServer.stubCreateExternalMovement()

        assertThat(restrictedPatientRepository.findById("A12345")).isPresent

        doThrow(RuntimeException()).whenever(domainEventPublisher).publishRestrictedPatientRemoved(any())

        webTestClient.delete().uri("/restricted-patient/prison-number/A12345")
          .headers(setHeaders())
          .exchange()
          .expectStatus().is5xxServerError

        assertThat(restrictedPatientRepository.findById("A12345")).isNotPresent

        verify(telemetryClient).trackEvent(
          "restricted-patient-removed",
          mapOf(
            "prisonerNumber" to "A12345",
            "fromLocationId" to "MDI",
            "hospitalLocationCode" to "HAZLWD",
            "supportingPrisonId" to "MDI",
            "dischargeTime" to "2020-10-09T00:00",
          ),
          null,
        )
      }
    }
  }

  @Nested
  @DisplayName("/change-supporting-prison")
  inner class ChangeSupportingPrison {
    @Test
    @WithMockAuthUser
    fun `change supporting prison`() {
      saveRestrictedPatient(prisonerNumber = "A12345", supportingPrisonId = "LEI")
      prisonApiMockServer.stubGetAgency("MDI", "INST")

      changeSupportingPrisonWebClient()
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.prisonerNumber").isEqualTo("A12345")
        .jsonPath("$.fromLocation.agencyId").isEqualTo("MDI")
        .jsonPath("$.hospitalLocation.agencyId").isEqualTo("HAZLWD")
        .jsonPath("$.supportingPrison.agencyId").isEqualTo("MDI")
        .jsonPath("$.dischargeTime").isEqualTo("2020-10-09T00:00:00")

      verify(domainEventPublisher).publishSupportingPrisonChanged("A12345")

      verify(telemetryClient).trackEvent(
        "restricted-patient-changed-supporting-prison",
        mapOf(
          "prisonerNumber" to "A12345",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI",
          "dischargeTime" to "2020-10-09T00:00",
        ),
        null,
      )
    }

    @Nested
    inner class ChangeSupportingPrisonErrors {
      @Test
      fun `should error if offender is not in prison`() {
        changeSupportingPrisonWebClient()
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("$.status").isEqualTo(404)
      }

      @Test
      @WithMockAuthUser
      fun `should error if supporting prison unchanged`() {
        saveRestrictedPatient(prisonerNumber = "A12345", supportingPrisonId = "MDI")

        changeSupportingPrisonWebClient(prisonerNumber = "A12345", supportingPrisonId = "MDI")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.status").isEqualTo(400)
          .jsonPath("$.errorCode").isEqualTo("PRISON_UNCHANGED")
      }

      @Test
      @WithMockAuthUser
      fun `should error if supporting prison not found`() {
        saveRestrictedPatient(prisonerNumber = "A12345", supportingPrisonId = "LEI")

        changeSupportingPrisonWebClient(prisonerNumber = "A12345", supportingPrisonId = "MDI")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.status").isEqualTo(400)
          .jsonPath("$.errorCode").isEqualTo("PRISON_NOT_FOUND")
      }

      @Test
      fun `should commit service transaction before publishing event`() {
        saveRestrictedPatient(prisonerNumber = "A12345", supportingPrisonId = "LEI")
        prisonApiMockServer.stubGetAgency("MDI", "INST")

        doThrow(RuntimeException()).whenever(domainEventPublisher).publishSupportingPrisonChanged(any())

        changeSupportingPrisonWebClient(prisonerNumber = "A12345", supportingPrisonId = "MDI")
          .exchange()
          .expectStatus().is5xxServerError

        assertThat(restrictedPatientRepository.findByIdOrNull("A12345")?.supportingPrisonId).isEqualTo("MDI")

        verify(telemetryClient).trackEvent(
          "restricted-patient-changed-supporting-prison",
          mapOf(
            "prisonerNumber" to "A12345",
            "fromLocationId" to "MDI",
            "hospitalLocationCode" to "HAZLWD",
            "supportingPrisonId" to "MDI",
            "dischargeTime" to "2020-10-09T00:00",
          ),
          null,
        )
      }
    }

    private fun changeSupportingPrisonWebClient(
      prisonerNumber: String = "A12345",
      supportingPrisonId: String = "MDI",
    ): WebTestClient.RequestHeadersSpec<*> {
      stubChangeSupportingPrison()

      return webTestClient
        .post()
        .uri("/change-supporting-prison")
        .headers(setHeaders())
        .bodyValue(
          mapOf(
            "offenderNo" to prisonerNumber,
            "supportingPrisonId" to supportingPrisonId,
          ),
        )
    }

    private fun stubChangeSupportingPrison() {
      prisonApiMockServer.stubAgencyLocationForPrisons()
      prisonApiMockServer.stubAgencyLocationForHospitals()
    }
  }
}
