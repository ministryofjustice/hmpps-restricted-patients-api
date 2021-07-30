package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

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
      .jsonPath("$.id").isNumber
      .jsonPath("$.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.fromLocation.agencyId").isEqualTo("MDI")
      .jsonPath("$.fromLocation.description").isEqualTo("Moorland (HMP & YOI)")
      .jsonPath("$.fromLocation.longDescription").isEqualTo("HMP & YOI Moorland Prison near Doncaster")
      .jsonPath("$.fromLocation.agencyType").isEqualTo("INST")
      .jsonPath("$.fromLocation.active").isEqualTo(true)
      .jsonPath("$.hospitalLocation.agencyId").isEqualTo("HAZLWD")
      .jsonPath("$.hospitalLocation.description").isEqualTo("Hazelwood House")
      .jsonPath("$.hospitalLocation.longDescription").isEqualTo("Hazelwood House")
      .jsonPath("$.hospitalLocation.agencyType").isEqualTo("HSHOSP")
      .jsonPath("$.hospitalLocation.active").isEqualTo(true)
      .jsonPath("$.supportingPrison.agencyId").isEqualTo("MDI")
      .jsonPath("$.supportingPrison.description").isEqualTo("Moorland (HMP & YOI)")
      .jsonPath("$.supportingPrison.longDescription").isEqualTo("HMP & YOI Moorland Prison near Doncaster")
      .jsonPath("$.supportingPrison.agencyType").isEqualTo("INST")
      .jsonPath("$.supportingPrison.active").isEqualTo(true)
      .jsonPath("$.dischargeTime").isEqualTo("2021-06-07T13:40:32.498")
      .jsonPath("$.commentText").isEqualTo("Prisoner was released on bail")

    prisonApiMockServer.verify(
      putRequestedFor(urlEqualTo("/api/offenders/A12345/discharge-to-hospital"))
        .withRequestBody(
          equalToJson(loadResourceFile("discharge-to-hospital-request.json"))
        )
    )

    prisonerSearchApiMockServer.verify(
      postRequestedFor(urlEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(
          equalToJson(loadResourceFile("prisoner-search-request.json"))
        )
    )
  }

  @Test
  fun `returns restricted patient by prisoner number`() {
    dischargePrisonerWebClient(prisonerNumber = "A16345")
      .exchange()
      .expectStatus().isCreated

    webTestClient.get().uri("/restricted-patient/prison-number/A16345")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .json(loadResourceFile("restricted-patient-by-prison-number-response.json"))
  }

  @Test
  fun `remove restricted patient`() {
    prisonApiMockServer.stubCreateExternalMovement()

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

    getRestrictedPatient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().isNotFound
  }
}
