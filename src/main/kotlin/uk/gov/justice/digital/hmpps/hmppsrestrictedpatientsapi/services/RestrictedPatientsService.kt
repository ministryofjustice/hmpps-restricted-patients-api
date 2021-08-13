package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions.NoResultsReturnedException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
class RestrictedPatientsService(
  @Qualifier("PrisonApiGatewayWithAuth") private val prisonApiGateway: PrisonApiGateway,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway,
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val telemetryClient: TelemetryClient,
  private val clock: Clock
) {

  @Transactional
  fun dischargeToHospital(dischargeToHospital: DischargeToHospitalRequest): RestrictedPatientDto {
    restrictedPatientsRepository.findByPrisonerNumber(dischargeToHospital.offenderNo)?.let {
      throw IllegalStateException("Prisoner (${dischargeToHospital.offenderNo}) is already a restricted patient")
    }

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
      dischargeTime = dischargeToHospitalWithDefaultSupportingPrison.dischargeTime,
      commentText = dischargeToHospitalWithDefaultSupportingPrison.commentText
    )

    val newRestrictedPatient = restrictedPatientsRepository.saveAndFlush(restrictedPatient)

    dischargeOrRollBackAndThrow(dischargeToHospitalWithDefaultSupportingPrison, newRestrictedPatient)

    return transform(
      newRestrictedPatient,
      Agency(agencyId = restrictedPatient.fromLocationId),
      Agency(agencyId = restrictedPatient.hospitalLocationCode),
      Agency(agencyId = restrictedPatient.supportingPrisonId),
    )
  }

  private fun dischargeOrRollBackAndThrow(
    dischargeToHospitalWithDefaultSupportingPrison: DischargeToHospitalRequest,
    newRestrictedPatient: RestrictedPatient
  ) {
    try {
      prisonApiGateway.dischargeToHospital(dischargeToHospitalWithDefaultSupportingPrison)
    } catch (e: Exception) {
      restrictedPatientsRepository.delete(newRestrictedPatient)
      throw e
    }
  }

  fun getRestrictedPatient(prisonerNumber: String): RestrictedPatientDto {
    val restrictedPatient = restrictedPatientsRepository.findByPrisonerNumber(prisonerNumber)
      ?: throw EntityNotFoundException("No restricted patient record found for prison number $prisonerNumber")

    val agencies = prisonApiGateway.getAgencyLocationsByType("INST")
    val hospitals =
      prisonApiGateway.getAgencyLocationsByType("HOSPITAL") + prisonApiGateway.getAgencyLocationsByType("HSHOSP")

    val prisonSentFrom = agencies.find { it.agencyId == restrictedPatient.fromLocationId }
    val supportingPrison = agencies.find { it.agencyId == restrictedPatient.supportingPrisonId }
    val hospitalSentTo = hospitals.find { it.agencyId == restrictedPatient.hospitalLocationCode }

    return transform(restrictedPatient, prisonSentFrom, hospitalSentTo, supportingPrison)
  }

  @Transactional
  fun removeRestrictedPatient(prisonerNumber: String) {
    val restrictedPatient = restrictedPatientsRepository.findByPrisonerNumber(prisonerNumber)
      ?: throw EntityNotFoundException("No restricted patient record found for prison number $prisonerNumber")

    val prisonerSearchResponse = prisonerSearchApiGateway.searchByPrisonNumber(prisonerNumber)

    val prisonerResult = prisonerSearchResponse.firstOrNull()
      ?: throw NoResultsReturnedException("No prisoner search results returned for $prisonerNumber")

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

    restrictedPatientsRepository.delete(restrictedPatient)

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
  }

  fun transform(
    restrictedPatient: RestrictedPatient,
    fromAgency: Agency? = null,
    toAgency: Agency? = null,
    supportingPrisonAgency: Agency? = null
  ): RestrictedPatientDto = RestrictedPatientDto(
    id = restrictedPatient.id!!,
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
