package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@ActiveProfiles("test")
class RestrictedPatientIntegrationTest : IntegrationTestBase() {

  @MockBean
  lateinit var clock: Clock

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
      .jsonPath("$.dischargeTime").isEqualTo("2021-06-07T13:40:32.498")
      .jsonPath("$.commentText").isEqualTo("Prisoner was released on bail")

    prisonApiMockServer.verify(
      putRequestedFor(urlEqualTo("/api/offenders/A12345/discharge-to-hospital"))
        .withRequestBody(
          equalToJson(loadResourceFile("discharge-to-hospital-request.json"))
        ).withHeader("Authorization", WireMock.containing("Bearer"))
    )

    prisonerSearchApiMockServer.verify(
      postRequestedFor(urlEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(
          equalToJson(loadResourceFile("prisoner-search-request.json"))
        )
        .withHeader("Authorization", WireMock.containing("Bearer"))
    )
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
