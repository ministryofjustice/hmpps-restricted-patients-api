package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.matching.UrlPattern

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

  fun stubGetLatestMovementsReleased(prisonerNumber: String, hospitalLocationCode: String) {
    stubFor(
      post(urlEqualTo("/api/movements/offenders?latestOnly=true&allBookings=false"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              [
               {
                 "offenderNo": "$prisonerNumber",
                 "createDateTime": "2022-05-20T14:36:13.980583319",
                 "fromAgency": "MDI",
                 "fromAgencyDescription": "Moorland (HMP & YOI)",
                 "toAgency": "$hospitalLocationCode",
                 "toAgencyDescription": "Avesbury House, Care UK",
                 "fromCity": "",
                 "toCity": "",
                 "movementType": "REL",
                 "movementTypeDescription": "Release",
                 "directionCode": "OUT",
                 "movementDate": "2022-05-20",
                 "movementTime": "14:36:13",
                 "movementReason": "Final Discharge To Hospital-Psychiatric",
                 "commentText": "Psychiatric Hospital Discharge to Avesbury House, Care UK"
                }
              ]
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetLatestMovementsAdmitted(prisonerNumber: String) {
    stubFor(
      post(urlEqualTo("/api/movements/offenders?latestOnly=true&allBookings=false"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              [
               {
                 "offenderNo": "$prisonerNumber",
                 "createDateTime": "2022-05-20T14:36:13.980583319",
                 "fromAgency": "CRT",
                 "fromAgencyDescription": "Some court",
                 "toAgency": "MDI",
                 "toAgencyDescription": "Moorland (HMP)",
                 "movementType": "ADM",
                 "movementTypeDescription": "Admission",
                 "directionCode": "IN",
                 "movementDate": "2022-05-20",
                 "movementTime": "14:36:13"
                }
              ]
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

  fun stubOffenderBooking(offenderNo: String, activeFlag: Boolean) {
    stubFor(
      get(urlPathEqualTo("/api/bookings/offenderNo/$offenderNo"))
        .withQueryParams(
          mapOf(
            "fullInfo" to equalTo("true"),
            "extraInfo" to equalTo("false"),
            "csraSummary" to equalTo("false")
          )
        )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
                {
                  "offenderNo": "$offenderNo",
                  "bookingId": 1234567,
                  "ignoredField": "ignored",
                  "activeFlag": "$activeFlag"
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubServerError(method: (urlPattern: UrlPattern) -> MappingBuilder) {
    stubFor(
      method(urlMatching("/api/.*"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(503)
        )
    )
  }
}
