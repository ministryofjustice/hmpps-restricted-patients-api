package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class PrisonApiMockServer : WireMockServer(8999) {
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

  fun stubDischargeToPrison(offenderNo: String) {
    stubFor(
      put(urlEqualTo("/api/offenders/$offenderNo/discharge-to-hospital"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              """
              {
                  "offenderNo": "G3417UE",
                  "bookingId": 960606,
                  "bookingNo": "P81288",
                  "offenderId": 2317754,
                  "rootOffenderId": 2317754,
                  "activeFlag": false,
                  "agencyId": "OUT",
                  "assignedLivingUnit": {
                      "agencyId": "OUT",
                      "agencyName": "Outside"
                  },
                  "inOutStatus": "OUT",
                  "status": "INACTIVE OUT",
                  "statusReason": "REL-HP",
                  "lastMovementTypeCode": "REL",
                  "lastMovementReasonCode": "HP",
                  "restrictivePatient": {
                      "supportingPrison": {
                          "agencyId": "MDI",
                          "description": "Moorland (HMP & YOI)",
                          "longDescription": "HMP & YOI Moorland Prison near Doncaster",
                          "agencyType": "INST",
                          "active": true
                      },
                      "dischargedHospital": {
                          "agencyId": "HAZLWD",
                          "description": "Hazelwood House",
                          "longDescription": "Hazelwood House",
                          "agencyType": "HSHOSP",
                          "active": true
                      },
                      "dischargeDate": "2021-06-07",
                      "dischargeDetails": "Psychiatric Hospital Discharge to Hazelwood House"
                  }
              }
              """.trimIndent()
            )
        )
    )
  }
}
