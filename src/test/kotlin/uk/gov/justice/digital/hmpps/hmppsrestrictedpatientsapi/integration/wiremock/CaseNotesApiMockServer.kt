package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class CaseNotesApiMockServer : WireMockServer(8102) {
  fun stubCreateCaseNote(offenderNumber: String) {
    stubFor(
      put(urlEqualTo("/case-notes/$offenderNumber"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
        )
    )
  }

  fun stubCreateCaseNotesError(offenderNumber: String) {
    stubFor(
      put(urlEqualTo("/case-notes/$offenderNumber"))
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
              """.trimIndent()
            )
        )
    )
  }
}
