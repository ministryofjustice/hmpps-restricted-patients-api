package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

class PrisonerSearchApiMockServer : WireMockServer(8100) {
  fun stubHealth() {
    stubFor(
      get(urlEqualTo("/health/ping"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                 "status": "UP"
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubSearchByPrisonNumber(
    prisonerNumber: String,
    response: String = """
      "prisonerNumber": "$prisonerNumber",
      "bookingId": "1138058",
      "legalStatus": "SENTENCED"
    """.trimIndent(),
  ) {
    stubFor(
      post(urlPathEqualTo("/prisoner-search/prisoner-numbers"))
        .withQueryParam("responseFields", containing("prisonerNumber"))
        .withQueryParam("responseFields", containing("bookingId"))
        .withQueryParam("responseFields", containing("conditionalReleaseDate"))
        .withQueryParam("responseFields", containing("sentenceExpiryDate"))
        .withQueryParam("responseFields", containing("recall"))
        .withQueryParam("responseFields", containing("indeterminateSentence"))
        .withQueryParam("responseFields", containing("legalStatus"))
        .withQueryParam("responseFields", containing("lastMovementTypeCode"))
        .withQueryParam("responseFields", containing("lastMovementReasonCode"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
             [ { $response } ]
              """.trimIndent(),
            ),
        ),
    )
  }
}
