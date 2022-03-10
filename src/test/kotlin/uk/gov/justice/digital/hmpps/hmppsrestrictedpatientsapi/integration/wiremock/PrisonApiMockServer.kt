package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class PrisonApiMockServer : WireMockServer(8989) {
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
                  "offenderNo": "$offenderNo",
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

  fun stubAgencyLocationForPrisons() {
    stubFor(
      get(urlEqualTo("/api/agencies/type/INST"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              [
               {
                  "agencyId": "MRI",
                  "description": "Manchester (HMP)",
                  "longDescription": "HMP MANCHESTER",
                  "agencyType": "INST",
                  "active": true
                },
                {
                  "agencyId": "MDI",
                  "description": "Moorland (HMP & YOI)",
                  "longDescription": "HMP & YOI Moorland Prison near Doncaster",
                  "agencyType": "INST",
                  "active": true
                },
                {
                  "agencyId": "HDI",
                  "description": "Hatfield (HMP & YOI)",
                  "longDescription": "HMP & YOI HATFIELD",
                  "agencyType": "INST",
                  "active": true
                }
              ]
              """.trimIndent()
            )
        )
    )
  }

  fun stubAgencyLocationForHospitals() {
    val response = """
         [
            {
              "agencyId": "STANSD",
              "description": "St Ann's (dorset)",
              "longDescription": "St Ann's (Dorset)",
              "agencyType": "HSHOSP",
              "active": true
            },
            {
              "agencyId": "BRADEN",
              "description": "Branden Unit",
              "longDescription": "Branden Unit",
              "agencyType": "HSHOSP",
              "active": true
            },
            {
              "agencyId": "HAZLWD",
              "description": "Hazelwood House",
              "longDescription": "Hazelwood House",
              "agencyType": "HSHOSP",
              "active": true
            }
         ]
    """.trimIndent()

    stubFor(
      get(urlEqualTo("/api/agencies/type/HSHOSP"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(response)
        )
    )
    stubFor(
      get(urlEqualTo("/api/agencies/type/HOSPITAL"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(response)
        )
    )
  }

  fun stubCreateExternalMovement() {
    stubFor(
      post(urlEqualTo("/api/movements"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
        )
    )
  }

  fun stubGetOffenderBooking(bookingId: Long, prisonerNumber: String) {
    stubFor(
      get(urlEqualTo("/api/bookings/$bookingId"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              {
                "bookingId": $bookingId,
                "offenderNo": "$prisonerNumber"
              }              
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetAgency(agencyId: String, agencyType: String, description: String) {
    stubFor(
      get(urlEqualTo("/api/agencies/$agencyId"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              {
                  "agencyId": "$agencyId",
                  "description": "$description",
                  "agencyType": "$agencyType",
                  "active": true
                }
              """.trimIndent()
            )
        )
    )
  }
}
