package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.MovementResponse

@Service
class PrisonApiGateway(private val prisonApiClientCreds: WebClient) {
  fun dischargeToHospital(newRestrictedPatient: RestrictedPatient) =
    prisonApiClientCreds
      .put()
      .uri("/offenders/${newRestrictedPatient.prisonerNumber}/discharge-to-hospital")
      .bodyValue(
        mapOf(
          "commentText" to newRestrictedPatient.commentText,
          "dischargeTime" to newRestrictedPatient.dischargeTime.toString(),
          "fromLocationId" to newRestrictedPatient.fromLocationId,
          "hospitalLocationCode" to newRestrictedPatient.hospitalLocationCode,
          "supportingPrisonId" to newRestrictedPatient.supportingPrisonId
        )
      )
      .retrieve()
      .bodyToMono(String::class.java)
      .block()

  fun getLatestMovements(offenderNo: String): List<MovementResponse> =
    prisonApiClientCreds
      .post()
      .uri("/movements/offenders?latestOnly=true&allBookings=true")
      .bodyValue(
        listOf(
          offenderNo
        )
      )
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<MovementResponse>>() {})
      .block()!!

  fun getAgencyLocationsByType(type: String): List<Agency> = prisonApiClientCreds
    .get()
    .uri("/agencies/type/$type")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<Agency>>() {})
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
}
