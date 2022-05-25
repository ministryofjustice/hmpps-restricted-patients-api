package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions.NoResultsReturnedException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.MigrateInRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.MovementResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

data class ExistingDischargeData(
  val dischargeTime: LocalDateTime,
  val fromLocationId: String,
  val comment: String?
)

@Service
class RestrictedPatientsService(
  private val prisonApiGateway: PrisonApiGateway,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway,
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val domainEventPublisher: DomainEventPublisher,
  private val telemetryClient: TelemetryClient,
  private val clock: Clock
) {

  @Transactional(value = Transactional.TxType.NOT_SUPPORTED)
  fun dischargeToHospital(dischargeToHospital: DischargeToHospitalRequest): RestrictedPatientDto {
    checkNotExistingPatient(dischargeToHospital.offenderNo)

    val prisonerSearchResponse = prisonerSearchApiGateway.searchByPrisonNumber(dischargeToHospital.offenderNo)

    prisonerSearchResponse.firstOrNull()
      ?: throw NoResultsReturnedException("No prisoner search results returned for ${dischargeToHospital.offenderNo}")

    val dischargeToHospitalWithDefaultSupportingPrison = dischargeToHospital.copy(
      supportingPrisonId = dischargeToHospital.supportingPrisonId ?: dischargeToHospital.fromLocationId
    )
    val restrictedPatient = RestrictedPatient(
      prisonerNumber = dischargeToHospitalWithDefaultSupportingPrison.offenderNo,
      fromLocationId = dischargeToHospitalWithDefaultSupportingPrison.fromLocationId,
      hospitalLocationCode = dischargeToHospitalWithDefaultSupportingPrison.hospitalLocationCode,
      supportingPrisonId = dischargeToHospitalWithDefaultSupportingPrison.supportingPrisonId!!,
      dischargeTime = LocalDateTime.now(clock),
      commentText = dischargeToHospitalWithDefaultSupportingPrison.commentText
    )
    return addRestrictedPatient(restrictedPatient)
  }

  @Transactional(value = Transactional.TxType.NOT_SUPPORTED)
  fun migrateInPatient(migrateIn: MigrateInRequest): RestrictedPatientDto {
    checkNotExistingPatient(migrateIn.offenderNo)

    val latestMovements = prisonApiGateway.getLatestMovements(migrateIn.offenderNo)
    val dischargeData = getExistingRestrictedPatientDischargeData(latestMovements, migrateIn.offenderNo)

    val restrictedPatientsToAdd = RestrictedPatient(
      prisonerNumber = migrateIn.offenderNo,
      fromLocationId = dischargeData.fromLocationId,
      hospitalLocationCode = migrateIn.hospitalLocationCode,
      supportingPrisonId = dischargeData.fromLocationId,
      dischargeTime = dischargeData.dischargeTime,
      commentText = dischargeData.comment
    )
    // Call Prison API as well as DB to ensure the data is consistent with discharges,
    // e.g. the movement reason is correct
    val addedPatient = addRestrictedPatient(restrictedPatientsToAdd)

    prisonerSearchApiGateway.refreshPrisonerIndex(migrateIn.offenderNo)
    return addedPatient
  }

  private fun checkNotExistingPatient(offenderNo: String) =
    restrictedPatientsRepository.findById(offenderNo).map {
      throw IllegalStateException("Prisoner ($offenderNo) is already a restricted patient")
    }

  private fun getExistingRestrictedPatientDischargeData(latestMovements: List<MovementResponse>, offenderNo: String): ExistingDischargeData {
    // These checks ensure the Prison API discharge call will just update the latest movement
    if (latestMovements.isEmpty()) {
      throw IllegalStateException("Prisoner ($offenderNo) does not have the correct latest movements to migrate")
    }
    if (latestMovements.size > 1) {
      throw RuntimeException("Prisoner ($offenderNo) has multiple latest movements")
    }
    val latestMovement = latestMovements[0]
    if ("REL" != latestMovement.movementType) {
      throw IllegalStateException("Prisoner ($offenderNo) was not released")
    }
    val fromAgencyId = latestMovement.fromAgency ?: throw IllegalStateException("Prisoner ($offenderNo) does not have an agency id associated with the last movement")
    val dischargeDateTime = calculateDischargeDateTime(offenderNo, latestMovement.movementDate, latestMovement.movementTime)
    return ExistingDischargeData(
      dischargeTime = dischargeDateTime,
      fromLocationId = fromAgencyId,
      comment = latestMovement.commentText,
    )
  }

  private fun calculateDischargeDateTime(offenderNo: String, movementDate: String?, movementTime: String?): LocalDateTime {
    try {
      return LocalDateTime.parse("${movementDate}T$movementTime")
    } catch (e: Exception) {
    }
    throw IllegalStateException("Prisoner ($offenderNo) does not have a valid movement date/time")
  }

  private fun addRestrictedPatient(restrictedPatient: RestrictedPatient): RestrictedPatientDto {
    val newRestrictedPatient = restrictedPatientsRepository.saveAndFlush(restrictedPatient)

    dischargeOrRollBackAndThrow(newRestrictedPatient)

    return transform(
      newRestrictedPatient,
      Agency(agencyId = restrictedPatient.fromLocationId),
      Agency(agencyId = restrictedPatient.hospitalLocationCode),
      Agency(agencyId = restrictedPatient.supportingPrisonId),
    )
  }

  private fun dischargeOrRollBackAndThrow(
    newRestrictedPatient: RestrictedPatient
  ) {
    try {
      prisonApiGateway.dischargeToHospital(newRestrictedPatient)
    } catch (e: Exception) {
      // We manually roll back because the Prisoner Offender Search
      // will call the API to get RP information before the transaction completes
      restrictedPatientsRepository.delete(newRestrictedPatient)
      throw e
    }
  }

  fun getRestrictedPatient(prisonerNumber: String): RestrictedPatientDto {
    val restrictedPatient = restrictedPatientsRepository.findById(prisonerNumber)
      .orElseThrow { EntityNotFoundException("No restricted patient record found for prison number $prisonerNumber") }

    val agencies = prisonApiGateway.getAgencyLocationsByType("INST")
    val hospitals =
      prisonApiGateway.getAgencyLocationsByType("HOSPITAL") + prisonApiGateway.getAgencyLocationsByType("HSHOSP")

    val prisonSentFrom = agencies.find { it.agencyId == restrictedPatient.fromLocationId }
    val supportingPrison = agencies.find { it.agencyId == restrictedPatient.supportingPrisonId }
    val hospitalSentTo = hospitals.find { it.agencyId == restrictedPatient.hospitalLocationCode }

    return transform(restrictedPatient, prisonSentFrom, hospitalSentTo, supportingPrison)
  }

  @Transactional(value = Transactional.TxType.NOT_SUPPORTED)
  fun removeRestrictedPatient(prisonerNumber: String) {
    val restrictedPatient = restrictedPatientsRepository.findById(prisonerNumber)
      .orElseThrow { throw EntityNotFoundException("No restricted patient record found for prison number $prisonerNumber") }

    val prisonerSearchResponse = prisonerSearchApiGateway.searchByPrisonNumber(prisonerNumber)

    val prisonerResult = prisonerSearchResponse.firstOrNull()
      ?: throw NoResultsReturnedException("No prisoner search results returned for $prisonerNumber")

    restrictedPatientsRepository.delete(restrictedPatient)

    try {
      prisonApiGateway.createExternalMovement(
        CreateExternalMovement(
          bookingId = prisonerResult.bookingId,
          fromAgencyId = restrictedPatient.hospitalLocationCode,
          toAgencyId = "OUT",
          movementTime = LocalDateTime.now(clock),
          movementType = "REL",
          movementReason = "CR",
          directionCode = "OUT"
        )
      )

      prisonerSearchApiGateway.refreshPrisonerIndex(prisonerNumber)

      domainEventPublisher.publishRestrictedPatientRemoved(prisonerNumber)

      telemetryClient.trackEvent(
        "restricted-patient-removed",
        mapOf(
          "prisonerNumber" to prisonerNumber,
          "fromLocationId" to restrictedPatient.fromLocationId,
          "hospitalLocationCode" to restrictedPatient.hospitalLocationCode,
          "supportingPrisonId" to restrictedPatient.supportingPrisonId,
          "dischargeTime" to restrictedPatient.dischargeTime.toString(),
        ),
        null
      )
    } catch (e: Exception) {
      restrictedPatientsRepository.saveAndFlush(restrictedPatient)
      throw e
    }
  }

  fun transform(
    restrictedPatient: RestrictedPatient,
    fromAgency: Agency? = null,
    toAgency: Agency? = null,
    supportingPrisonAgency: Agency? = null
  ): RestrictedPatientDto = RestrictedPatientDto(
    prisonerNumber = restrictedPatient.prisonerNumber,
    fromLocation = fromAgency,
    supportingPrison = supportingPrisonAgency,
    dischargeTime = restrictedPatient.dischargeTime,
    commentText = restrictedPatient.commentText,
    hospitalLocation = toAgency,
    createUserId = restrictedPatient.createUserId,
    createDateTime = restrictedPatient.createDateTime
  )
}
