package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions.NoResultsReturnedException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional
import javax.validation.ValidationException

@Service
class RestrictedPatientsService(
  private val prisonApiGateway: PrisonApiGateway,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway,
  private val restrictedPatientsRepository: RestrictedPatientsRepository
) {

  @Transactional
  fun dischargeToHospital(dischargeToHospital: DischargeToHospitalRequest): RestrictedPatientDto {

    val prisonerSearchResponse = prisonerSearchApiGateway.searchByPrisonNumber(dischargeToHospital.offenderNo)

    val prisonerResult = prisonerSearchResponse.firstOrNull()
      ?: throw NoResultsReturnedException("No prisoner search results returned for ${dischargeToHospital.offenderNo}")

    if (!isCorrectLegalStatus(prisonerResult.legalStatus))
      throw ValidationException("Can not discharge prisoner with a legal status of ${prisonerResult.legalStatus}")

    val response = prisonApiGateway.dischargeToHospital(dischargeToHospital)

    val restrictedPatient = RestrictedPatient(
      prisonerNumber = dischargeToHospital.offenderNo,
      fromLocationId = dischargeToHospital.fromLocationId,
      hospitalLocationCode = dischargeToHospital.hospitalLocationCode,
      supportingPrisonId = dischargeToHospital.supportingPrisonId,
      dischargeTime = dischargeToHospital.dischargeTime,
      commentText = dischargeToHospital.commentText
    )

    return transform(
      restrictedPatientsRepository.save(restrictedPatient),
      response?.restrictivePatient?.supportingPrison,
      response?.restrictivePatient?.dischargedHospital,
      response?.restrictivePatient?.supportingPrison
    )
  }

  fun getRestrictedPatient(prisonerNumber: String): RestrictedPatientDto {
    val restrictedPatient = restrictedPatientsRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber)
      ?: throw EntityNotFoundException("No restricted patient record found for prison number $prisonerNumber")

    val agencies = prisonApiGateway.getAgencyLocationsByType("INST")
    val hospitals =
      prisonApiGateway.getAgencyLocationsByType("HOSPITAL") + prisonApiGateway.getAgencyLocationsByType("HSHOSP")

    val prisonSentFrom = agencies.find { it.agencyId == restrictedPatient.fromLocationId }
    val supportingPrison = agencies.find { it.agencyId == restrictedPatient.supportingPrisonId }
    val hospitalSentTo = hospitals.find { it.agencyId == restrictedPatient.hospitalLocationCode }

    return transform(restrictedPatient, prisonSentFrom, hospitalSentTo, supportingPrison)
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
    active = restrictedPatient.active,
    createUserId = restrictedPatient.createUserId,
    createDateTime = restrictedPatient.createDateTime
  )

  private fun isCorrectLegalStatus(legalStatus: LegalStatus?): Boolean =
    LegalStatus.permissibleLegalStatusesForDischargingPrisonersToHospital().contains(legalStatus)
}
