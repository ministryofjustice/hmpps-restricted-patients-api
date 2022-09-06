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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class RestrictedPatientIntegrationTest : IntegrationTestBase() {

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
          equalToJson(loadResourceFile("discharge-to-hospital-request.json"))
        ).withHeader("Authorization", WireMock.containing("Bearer"))
    )

    prisonApiMockServer.verify(
      getRequestedFor(urlPathEqualTo("/api/bookings/offenderNo/A12345"))
        .withQueryParam("fullInfo", equalTo("true"))
        .withQueryParam("extraInfo", equalTo("false"))
        .withQueryParam("csraSummary", equalTo("false"))
        .withHeader("Authorization", WireMock.containing("Bearer"))
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
  }

  @Test
  fun `migrate in a patient that has been moved out using NOMIS only`() {
    val rpEntryBeforeTest = restrictedPatientRepository.findById("A12345")
    assertFalse(rpEntryBeforeTest.isPresent)

    migrateInRestrictedPatientWebClient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.fromLocation.agencyId").isEqualTo("MDI")
      .jsonPath("$.hospitalLocation.agencyId").isEqualTo("HAZLWD")
      .jsonPath("$.supportingPrison.agencyId").isEqualTo("MDI")
      .jsonPath("$.dischargeTime").isEqualTo("2022-05-20T14:36:13")
      .jsonPath("$.commentText").isEqualTo("Psychiatric Hospital Discharge to Avesbury House, Care UK")

    prisonApiMockServer.verify(
      postRequestedFor(urlEqualTo("/api/movements/offenders?latestOnly=true&allBookings=false"))
        .withRequestBody(
          equalToJson("""["A12345"]""")
        ).withHeader("Authorization", WireMock.containing("Bearer"))
    )

    prisonerSearchApiMockServer.verify(
      putRequestedFor(urlEqualTo("/prisoner-index/index/prisoner/A12345"))
    )

    val rpEntry = restrictedPatientRepository.findById("A12345")
    assertTrue(rpEntry.isPresent)
  }

  @Nested
  inner class MigrateInPatientErrors {

    @Test
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
  }

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

  @Test
  fun `remove restricted patient`() {
    prisonApiMockServer.stubCreateExternalMovement()
    prisonerSearchApiMockServer.stubRefreshIndex("A12345")
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
          equalToJson(loadResourceFile("create-external-movement-request.json"))
        )
    )

    prisonerSearchApiMockServer.verify(
      putRequestedFor(urlEqualTo("/prisoner-index/index/prisoner/A12345"))
    )

    getRestrictedPatient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().isNotFound
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
    fun `should error if prison-api errors`() {
      saveRestrictedPatient(prisonerNumber = "A12345")
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
  }
}
