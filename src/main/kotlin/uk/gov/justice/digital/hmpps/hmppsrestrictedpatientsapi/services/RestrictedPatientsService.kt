package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.BadRequestException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers.SupportingPrisonRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.MigrateInRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.MovementResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.Clock
import java.time.LocalDateTime

data class ExistingDischargeData(
  val dischargeTime: LocalDateTime,
  val fromLocationId: String,
  val comment: String?,
)

@Service
@Transactional(readOnly = true)
class RestrictedPatientsService(
  private val prisonApiGateway: PrisonApiGateway,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway,
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val clock: Clock,
) {

  @Transactional
  fun dischargeToHospital(dischargeToHospital: DischargeToHospitalRequest): RestrictedPatientDto {
    checkNotExistingPatient(dischargeToHospital.offenderNo)

    prisonApiGateway.getOffenderBooking(dischargeToHospital.offenderNo)
      ?.takeIf { it.activeFlag }
      ?: throw EntityNotFoundException("No prisoner with activeFlag 'Y' found for ${dischargeToHospital.offenderNo}")

    val dischargeToHospitalWithDefaultSupportingPrison = dischargeToHospital.copy(
      supportingPrisonId = dischargeToHospital.supportingPrisonId ?: dischargeToHospital.fromLocationId,
    )
    val restrictedPatient = RestrictedPatient(
      prisonerNumber = dischargeToHospitalWithDefaultSupportingPrison.offenderNo,
      fromLocationId = dischargeToHospitalWithDefaultSupportingPrison.fromLocationId,
      hospitalLocationCode = dischargeToHospitalWithDefaultSupportingPrison.hospitalLocationCode,
      supportingPrisonId = dischargeToHospitalWithDefaultSupportingPrison.supportingPrisonId!!,
      dischargeTime = dischargeToHospital.dischargeTime ?: LocalDateTime.now(clock),
      commentText = dischargeToHospitalWithDefaultSupportingPrison.commentText,
    )
    return addRestrictedPatient(restrictedPatient)
  }

  @Transactional
  fun migrateInPatient(migrateIn: MigrateInRequest): RestrictedPatientDto {
    checkNotExistingPatient(migrateIn.offenderNo)

    val dischargeData = getLatestMovement(migrateIn.offenderNo)
      .also { checkLatestMovementOkForDischarge(it, migrateIn.offenderNo) }
      .let { getExistingRestrictedPatientDischargeData(it, migrateIn.offenderNo) }

    return RestrictedPatient(
      prisonerNumber = migrateIn.offenderNo,
      fromLocationId = dischargeData.fromLocationId,
      hospitalLocationCode = migrateIn.hospitalLocationCode,
      supportingPrisonId = dischargeData.fromLocationId,
      dischargeTime = dischargeData.dischargeTime,
      commentText = "Historical discharge to hospital added to restricted patients",
    )
      .let { addRestrictedPatient(it) }
  }

  private fun checkNotExistingPatient(offenderNo: String) = restrictedPatientsRepository.findById(offenderNo).map {
    throw BadRequestException(
      errorCode = "EXISTING_PATIENT",
      message = "Prisoner ($offenderNo) is already a restricted patient",
    )
  }

  private fun getLatestMovement(offenderNo: String): MovementResponse {
    val movements = prisonApiGateway.getLatestMovements(offenderNo)

    if (movements.isEmpty()) {
      throw BadRequestException(
        errorCode = "NO_MOVEMENTS",
        message = "Prisoner ($offenderNo) does not have the correct latest movements to migrate",
      )
    }
    if (movements.size > 1) {
      throw BadRequestException(
        errorCode = "MULTIPLE_MOVEMENTS",
        message = "Prisoner ($offenderNo) has multiple latest movements",
      )
    }
    return movements[0]
  }

  private fun checkLatestMovementOkForDischarge(latestMovement: MovementResponse, offenderNo: String) {
    if ("REL" != latestMovement.movementType) {
      throw BadRequestException(errorCode = "LAST_MOVE_NOT_REL", message = "Prisoner ($offenderNo) was not released")
    }
    latestMovement.fromAgency ?: throw BadRequestException(
      errorCode = "LAST_MOVE_NO_FROM_AGENCY",
      message = "Prisoner ($offenderNo) does not have an agency id associated with the last movement",
    )
  }

  private fun getExistingRestrictedPatientDischargeData(
    latestMovement: MovementResponse,
    offenderNo: String,
  ): ExistingDischargeData {
    val dischargeDateTime =
      calculateDischargeDateTime(offenderNo, latestMovement.movementDate, latestMovement.movementTime)
    return ExistingDischargeData(
      dischargeTime = dischargeDateTime,
      fromLocationId = latestMovement.fromAgency!!,
      comment = latestMovement.commentText,
    )
  }

  private fun addRestrictedPatient(restrictedPatient: RestrictedPatient): RestrictedPatientDto = restrictedPatientsRepository.save(restrictedPatient).run {
    prisonApiGateway.dischargeToHospital(this)
    transform(
      this,
      Agency(agencyId = fromLocationId),
      Agency(agencyId = hospitalLocationCode),
      Agency(agencyId = supportingPrisonId),
    )
  }

  fun getRestrictedPatient(prisonerNumber: String): RestrictedPatientDto = restrictedPatientsRepository.findByIdOrNull(prisonerNumber)
    ?.let { transformIntoDto(it) }
    ?: throw EntityNotFoundException("No restricted patient record found for prison number $prisonerNumber")

  private fun transformIntoDto(restrictedPatient: RestrictedPatient): RestrictedPatientDto {
    val agencies = prisonApiGateway.getAgencyLocationsByType("INST")
    val hospitals =
      prisonApiGateway.getAgencyLocationsByType("HOSPITAL") + prisonApiGateway.getAgencyLocationsByType("HSHOSP")

    val prisonSentFrom = agencies.find { it.agencyId == restrictedPatient.fromLocationId }
    val supportingPrison = agencies.find { it.agencyId == restrictedPatient.supportingPrisonId }
    val hospitalSentTo = hospitals.find { it.agencyId == restrictedPatient.hospitalLocationCode }

    return transform(restrictedPatient, prisonSentFrom, hospitalSentTo, supportingPrison)
  }

  @Transactional
  fun removeRestrictedPatient(prisonerNumber: String): RestrictedPatient {
    val restrictedPatient = restrictedPatientsRepository.findById(prisonerNumber)
      .orElseThrow { throw EntityNotFoundException("No restricted patient record found for prison number $prisonerNumber") }

    val prisonerResult = prisonerSearchApiGateway.searchByPrisonNumber(prisonerNumber).firstOrNull()
      ?: throw EntityNotFoundException("No prisoner search results returned for $prisonerNumber")

    restrictedPatientsRepository.delete(restrictedPatient)

    return restrictedPatient.also {
      prisonApiGateway.createExternalMovement(
        CreateExternalMovement(
          bookingId = prisonerResult.bookingId,
          fromAgencyId = restrictedPatient.hospitalLocationCode,
          toAgencyId = "OUT",
          movementTime = LocalDateTime.now(clock),
          movementType = "REL",
          movementReason = "CR",
          directionCode = "OUT",
        ),
      )
    }
  }

  @Transactional
  fun changeSupportingPrison(request: SupportingPrisonRequest): RestrictedPatientDto {
    val restrictedPatient = restrictedPatientsRepository.findById(request.offenderNo)
      .orElseThrow { throw EntityNotFoundException("No restricted patient record found for prison number ${request.offenderNo}") }

    if (restrictedPatient.supportingPrisonId == request.supportingPrisonId) {
      throw BadRequestException(
        errorCode = "PRISON_UNCHANGED",
        message = "Prisoner (${request.offenderNo}) already supported by ${request.supportingPrisonId}",
      )
    }
    if (prisonApiGateway.getAgency(request.supportingPrisonId) == null) {
      throw BadRequestException(
        errorCode = "PRISON_NOT_FOUND",
        message = "Prison ${request.supportingPrisonId} not found",
      )
    }

    restrictedPatient.supportingPrisonId = request.supportingPrisonId
    return transformIntoDto(restrictedPatientsRepository.save(restrictedPatient))
  }

  private fun transform(
    restrictedPatient: RestrictedPatient,
    fromAgency: Agency? = null,
    toAgency: Agency? = null,
    supportingPrisonAgency: Agency? = null,
  ): RestrictedPatientDto = RestrictedPatientDto(
    prisonerNumber = restrictedPatient.prisonerNumber,
    fromLocation = fromAgency,
    supportingPrison = supportingPrisonAgency,
    dischargeTime = restrictedPatient.dischargeTime,
    commentText = restrictedPatient.commentText,
    hospitalLocation = toAgency,
    createUserId = restrictedPatient.createUserId,
    createDateTime = restrictedPatient.createDateTime,
  )
}

fun calculateDischargeDateTime(
  offenderNo: String,
  movementDate: String?,
  movementTime: String?,
): LocalDateTime {
  try {
    return LocalDateTime.parse("${movementDate}T$movementTime")
  } catch (e: Exception) {
    throw BadRequestException(
      errorCode = "DISCHARGE_TIME_INVALID",
      message = "Prisoner ($offenderNo) does not have a valid movement date/time",
    )
  }
}
