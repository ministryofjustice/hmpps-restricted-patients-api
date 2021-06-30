package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

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
              """.trimIndent()
            )
        )
    )
  }

  fun stubSearchByPrisonNumber() {
    stubFor(
      post(urlEqualTo("/prisoner-search/prisoner-numbers"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
             [
                {
                  "prisonerNumber": "G1670VU",
                  "pncNumber": "96/371915Q",
                  "pncNumberCanonicalShort": "96/371915Q",
                  "pncNumberCanonicalLong": "1996/371915Q",
                  "croNumber": "177155/96K",
                  "bookingId": "1138058",
                  "bookNumber": "W73895",
                  "firstName": "EMANETTA",
                  "middleNames": "PATES",
                  "lastName": "ABOLD",
                  "dateOfBirth": "1984-08-22",
                  "legalStatus": "SENTENCED"
                }
               ]
              """.trimIndent()
            )
        )
    )
  }
}
