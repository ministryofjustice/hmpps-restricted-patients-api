package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.DischargeToHospitalResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse

@Component("PrisonApiGatewayWithAuth")
class PrisonApiGateway(private val prisonApiWithAuthWebClient: WebClient) {
  fun dischargeToHospital(dischargeToHospitalDetails: DischargeToHospitalRequest): DischargeToHospitalResponse? =
    prisonApiWithAuthWebClient
      .put()
      .uri("/offenders/${dischargeToHospitalDetails.offenderNo}/discharge-to-hospital")
      .bodyValue(
        mapOf(
          "commentText" to dischargeToHospitalDetails.commentText,
          "dischargeTime" to dischargeToHospitalDetails.dischargeTime,
          "fromLocationId" to dischargeToHospitalDetails.fromLocationId,
          "hospitalLocationCode" to dischargeToHospitalDetails.hospitalLocationCode,
          "supportingPrisonId" to dischargeToHospitalDetails.supportingPrisonId
        )
      )
      .retrieve()
      .bodyToMono(DischargeToHospitalResponse::class.java)
      .block()

  fun getAgencyLocationsByType(type: String): List<Agency> = prisonApiWithAuthWebClient
    .get()
    .uri("/agencies/type/$type")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<Agency>>() {})
    .block()!!

  fun createExternalMovement(createExternalMovement: CreateExternalMovement) {
    prisonApiWithAuthWebClient
      .post()
      .uri("/movements")
      .bodyValue(createExternalMovement)
      .retrieve()
      .bodyToMono(String::class.java)
      .block()
  }

  fun getOffenderBooking(bookingId: Long): OffenderBookingResponse = prisonApiWithAuthWebClient
    .get()
    .uri("/api/booings/$bookingId")
    .retrieve()
    .bodyToMono(OffenderBookingResponse::class.java)
    .block()!!
}

@Component("PrisonApiGatewayClientCreds")
class PrisonApiGatewayClientCreds(prisonApiClientCredsWebclient: WebClient) :
  PrisonApiGateway(prisonApiClientCredsWebclient)
