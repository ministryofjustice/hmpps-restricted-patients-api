package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest

@Component
class PrisonApiGateway(private val prisonApiWithAuthWebClient: WebClient) {
  fun dischargeToHospital(dischargeToHospitalDetails: DischargeToHospitalRequest) {
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
      .bodyToMono<Any>()
      .block()
  }
}
