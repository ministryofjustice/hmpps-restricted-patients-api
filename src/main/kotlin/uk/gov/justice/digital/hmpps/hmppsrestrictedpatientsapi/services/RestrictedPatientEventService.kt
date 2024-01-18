package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.MovementResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository

@Service
class RestrictedPatientEventService(
  private val prisonApiApplicationGateway: PrisonApiGateway,
  private val domainEventPublisher: DomainEventPublisher,
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
) {

  @Transactional
  fun prisonerReleased(offenderNo: String, reason: String) {
    if (reason != "RELEASED_TO_HOSPITAL") {
      return
    }

    val lastMovement = getReleaseToHospitalMovement(offenderNo)
    if (lastMovement == null) {
      log.info("Ignoring release of prisoner {} as last movement not to a hospital", offenderNo)
      return
    }

    val hospital = getHospital(lastMovement)
    if (hospital == null) {
      log.error("Ignoring release of prisoner {} due to unrecognised hospital {}", offenderNo, lastMovement.toAgency)
      return
    }

    if (restrictedPatientsRepository.existsById(offenderNo)) {
      log.info("Ignoring release of prisoner {} as they are already a restricted patient", offenderNo)
      return
    }

    restrictedPatientsRepository.save(
      RestrictedPatient(
        prisonerNumber = offenderNo,
        fromLocationId = lastMovement.fromAgency!!,
        hospitalLocationCode = hospital.agencyId,
        supportingPrisonId = lastMovement.fromAgency,
        dischargeTime = calculateDischargeDateTime(offenderNo, lastMovement.movementDate, lastMovement.movementTime),
        commentText = lastMovement.commentText,
      ),
    )
    domainEventPublisher.publishRestrictedPatientAdded(offenderNo)
  }

  private fun getHospital(lastMovement: MovementResponse): Agency? =
    lastMovement
      .takeIf { it.toAgency != null }
      ?.let { prisonApiApplicationGateway.getAgency(it.toAgency!!) }
      ?.takeIf { agency -> listOf("HOSPITAL", "HSHOSP").contains(agency.agencyType) }

  private fun getReleaseToHospitalMovement(offenderNo: String): MovementResponse? =
    prisonApiApplicationGateway.getLatestMovements(offenderNo)
      .lastOrNull()
      ?.takeIf { it.movementType == "REL" && it.movementReasonCode == "HP" }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
