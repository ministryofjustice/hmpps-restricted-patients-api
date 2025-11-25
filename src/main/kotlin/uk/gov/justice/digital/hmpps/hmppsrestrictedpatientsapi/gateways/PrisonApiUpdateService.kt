package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement

@Service
class PrisonApiUpdateService(
  @Qualifier("prisonApiUserClient") private val prisonApiUserClient: WebClient? = null,
  @Qualifier("prisonApiSystemClient") private val prisonApiSystemClient: WebClient,
) {
  // We attempt to use credentials including a user if available (e.g. from a web request), but fall back to the system client credentials if not (e.g. for event processing or batch jobs)
  private fun prisonApiClient() = prisonApiUserClient ?: prisonApiSystemClient

  fun dischargeToHospital(newRestrictedPatient: RestrictedPatient): InmateDetail = prisonApiClient()
    .put()
    .uri("/offenders/{prisonerNumber}/discharge-to-hospital", newRestrictedPatient.prisonerNumber)
    .bodyValue(
      mapOf(
        "dischargeTime" to newRestrictedPatient.dischargeTime.toString(),
        "fromLocationId" to newRestrictedPatient.fromLocationId,
        "hospitalLocationCode" to newRestrictedPatient.hospitalLocationCode,
        "supportingPrisonId" to newRestrictedPatient.supportingPrisonId,
      ),
    )
    .retrieve()
    .bodyToMono<InmateDetail>()
    .block()!!

  fun createExternalMovement(createExternalMovement: CreateExternalMovement) {
    prisonApiClient()
      .post()
      .uri("/movements")
      .bodyValue(createExternalMovement)
      .retrieve()
      .bodyToMono(String::class.java)
      .block()
  }
}
