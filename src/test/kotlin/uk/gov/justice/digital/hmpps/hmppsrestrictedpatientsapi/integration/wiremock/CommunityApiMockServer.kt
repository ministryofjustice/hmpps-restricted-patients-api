package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class CommunityApiMockServer : WireMockServer(8101) {
  fun stubUpdateNomsNumber(crn: String) {
    stubFor(
      put(urlEqualTo("/secure/offenders/crn/$crn/nomsNumber"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubUpdateNomsNumberError(crn: String) {
    stubFor(
      put(urlEqualTo("/secure/offenders/crn/$crn/nomsNumber"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(400)
            .withBody(
              """
              {
                  "status": 400,
                  "userMessage": "some error",
              }
              """.trimIndent(),
            ),
        ),
    )
  }
}
