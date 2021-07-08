package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient

class RestrictedPatientIntegrationTest : IntegrationTestBase() {

  @Test
  fun `discharge a prisoner to hospital`() {
    prisonApiMockServer.stubDischargeToPrison("A12345")
    prisonerSearchApiMockServer.stubSearchByPrisonNumber()

    dischargePrisonerWebClient(offenderNo = "A12345")
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.id").isNumber
      .jsonPath("$.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.fromLocationId").isEqualTo("MDI")
      .jsonPath("$.hospitalLocationCode").isEqualTo("HAZLWD")
      .jsonPath("$.supportingPrisonId").isEqualTo("MDI")
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
    prisonApiMockServer.stubDischargeToPrison("A16345")
    prisonerSearchApiMockServer.stubSearchByPrisonNumber()

    dischargePrisonerWebClient(offenderNo = "A16345")
      .exchange()
      .expectStatus().isCreated

    webTestClient.get().uri("/restricted-patient/prison-number/A16345")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .json(loadResourceFile("restricted-patient-by-prison-number-response.json"))
  }

  fun dischargePrisonerWebClient(
    offenderNo: String,
    commentText: String = "Prisoner was released on bail",
    dischargeTime: String = "2021-06-07T13:40:32.498Z",
    fromLocationId: String = "MDI",
    hospitalLocationCode: String = "HAZLWD",
    supportingPrisonId: String = "MDI"

  ): WebTestClient.RequestHeadersSpec<*> = webTestClient
    .post()
    .uri("/discharge-to-hospital")
    .headers(setHeaders())
    .bodyValue(
      mapOf(
        "offenderNo" to offenderNo,
        "commentText" to commentText,
        "dischargeTime" to dischargeTime,
        "fromLocationId" to fromLocationId,
        "hospitalLocationCode" to hospitalLocationCode,
        "supportingPrisonId" to supportingPrisonId
      )
    )
}
