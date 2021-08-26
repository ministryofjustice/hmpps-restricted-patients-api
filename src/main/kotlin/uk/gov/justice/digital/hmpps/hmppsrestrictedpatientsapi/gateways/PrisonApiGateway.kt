package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse

@Service
class PrisonApiGateway(private val prisonApiClientCreds: WebClient) {
  fun dischargeToHospital(dischargeToHospitalDetails: DischargeToHospitalRequest) =
    prisonApiClientCreds
      .put()
      .uri("/offenders/${dischargeToHospitalDetails.offenderNo}/discharge-to-hospital")
      .bodyValue(
        mapOf(
          "commentText" to dischargeToHospitalDetails.commentText,
          "dischargeTime" to dischargeToHospitalDetails.dischargeTime.toString(),
          "fromLocationId" to dischargeToHospitalDetails.fromLocationId,
          "hospitalLocationCode" to dischargeToHospitalDetails.hospitalLocationCode,
          "supportingPrisonId" to dischargeToHospitalDetails.supportingPrisonId
        )
      )
      .retrieve()
      .bodyToMono(String::class.java)
      .block()

  fun getAgencyLocationsByType(type: String): List<Agency> = prisonApiClientCreds
    .get()
    .uri("/agencies/type/$type")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<Agency>>() {})
    .block()!!

  fun getAgencyById(agencyId: String): Agency? = prisonApiClientCreds
    .get()
    .uri("/agencies/$agencyId")
    .retrieve()
    .bodyToMono(Agency::class.java)
    .block()!!

  fun createExternalMovement(createExternalMovement: CreateExternalMovement) {
    prisonApiClientCreds
      .post()
      .uri("/movements")
      .bodyValue(createExternalMovement)
      .retrieve()
      .bodyToMono(String::class.java)
      .block()
  }

  fun getOffenderBooking(bookingId: Long): OffenderBookingResponse = prisonApiClientCreds
    .get()
    .uri("/bookings/$bookingId")
    .retrieve()
    .bodyToMono(OffenderBookingResponse::class.java)
    .block()!!
}
