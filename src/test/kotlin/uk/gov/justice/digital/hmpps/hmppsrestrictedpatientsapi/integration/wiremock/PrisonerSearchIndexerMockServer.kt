package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class PrisonerSearchIndexerMockServer : WireMockServer(8103) {
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

  fun stubRefreshIndex(prisonerNumber: String) {
    stubFor(
      put(urlEqualTo("/maintain-index/index-prisoner/$prisonerNumber"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """             
                {
                  "prisonerNumber": "$prisonerNumber",
                  "pncNumber": "96/371915Q",
                  "pncNumberCanonicalShort": "96/371915Q",
                  "pncNumberCanonicalLong": "1996/371915Q",
                  "croNumber": "177155/96K",
                  "bookingId": "1138058",
                  "legalStatus": "UNKNOWN"
                }               
              """.trimIndent(),
            ),
        ),
    )
  }
}
