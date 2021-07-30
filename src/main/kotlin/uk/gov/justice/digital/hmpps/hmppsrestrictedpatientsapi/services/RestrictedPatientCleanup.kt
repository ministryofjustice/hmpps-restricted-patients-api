package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
class RestrictedPatientCleanup(
  @Qualifier("PrisonApiGatewayClientCreds") private val prisonApiGateway: PrisonApiGateway,
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val telemetryClient: TelemetryClient
) {

  @Transactional
  fun deleteRestrictedPatientOnExternalMovementIntoPrison(externalPrisonerMovementMessage: ExternalPrisonerMovementMessage) {
    if (externalPrisonerMovementMessage.directionCode != "IN") return

    val isValidPrison = prisonApiGateway.getAgencyLocationsByType("INST")
      .filter { it.active }
      .map { it.agencyId }
      .contains(externalPrisonerMovementMessage.toAgencyLocationId)

    if (!isValidPrison) return

    val bookingId = externalPrisonerMovementMessage.bookingId
    val prisonerNumber = tryGetPrisonerNumber(bookingId) ?: return
    val restrictedPatient = tryGetRestrictedPatient(prisonerNumber) ?: return

    restrictedPatientsRepository.delete(restrictedPatient)

    telemetryClient.trackEvent(
      "restricted-patient-removed-on-external-movement-in",
      mapOf(
        "prisonerNumber" to prisonerNumber,
        "fromLocationId" to restrictedPatient.fromLocationId,
        "hospitalLocationCode" to restrictedPatient.hospitalLocationCode,
        "supportingPrisonId" to restrictedPatient.supportingPrisonId,
        "dischargeTime" to restrictedPatient.dischargeTime.toString(),
      ),
      null
    )
  }

  private fun tryGetRestrictedPatient(prisonerNumber: String): RestrictedPatient? = try {
    restrictedPatientsRepository.findByPrisonerNumber(prisonerNumber)
  } catch (e: EntityNotFoundException) {
    null
  }

  fun tryGetPrisonerNumber(bookingId: Long): String? = try {
    prisonApiGateway.getOffenderBooking(bookingId).offenderNo
  } catch (e: Exception) {
    null
  }
}
