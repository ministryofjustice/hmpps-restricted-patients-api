package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.Test

class RestrictedPatientsIntegrationTest : IntegrationTestBase() {
  @Test
  fun `calls discharge to hospital prison api endpoint`() {
    prisonApiMockServer.stubDischargeToPrison("A12345")

    webTestClient
      .post()
      .uri("/discharge-to-hospital")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "offenderNo" to "A12345",
          "commentText" to "Prisoner was released on bail",
          "dischargeTime" to "2021-06-07T13:40:32.498Z",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI"
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .json(loadResourceFile("discharge-to-hospital-response.json"))

    prisonApiMockServer.verify(
      WireMock.putRequestedFor(WireMock.urlEqualTo("/api/offenders/A12345/discharge-to-hospital"))
        .withRequestBody(
          WireMock.equalToJson(
            """
          {
            "commentText": "Prisoner was released on bail",
            "dischargeTime": "2021-06-07T13:40:32.498",
            "fromLocationId": "MDI",
            "hospitalLocationCode": "HAZLWD",
            "supportingPrisonId": "MDI"
          }
            """.trimIndent()
          )
        )
    )
  }
}
