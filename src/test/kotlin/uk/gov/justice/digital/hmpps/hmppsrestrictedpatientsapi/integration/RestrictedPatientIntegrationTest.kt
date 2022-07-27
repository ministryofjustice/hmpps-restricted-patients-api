package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@ActiveProfiles("test")
class RestrictedPatientIntegrationTest : IntegrationTestBase() {

  @MockBean
  lateinit var clock: Clock

  @Autowired
  lateinit var restrictedPatientRepository: RestrictedPatientsRepository

  @BeforeEach
  fun beforeEach() {
    val fixedClock =
      Clock.fixed(
        LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
      )
    whenever(clock.instant()).thenReturn(fixedClock.instant())
    whenever(clock.getZone()).thenReturn(fixedClock.getZone())
  }

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
}
