package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictivePatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions.NoResultsReturnedException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.DischargeToHospitalResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import javax.transaction.Transactional
import javax.validation.ValidationException

@Service
class RestrictedPatientsService(
  private val prisonApiGateway: PrisonApiGateway,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway,
  private val restrictedPatientsRepository: RestrictedPatientsRepository
) {

  @Transactional
  fun dischargeToHospital(dischargeToHospital: DischargeToHospitalRequest): DischargeToHospitalResponse {

    val prisonerSearchResponse = prisonerSearchApiGateway.searchByPrisonNumber(dischargeToHospital.offenderNo)

    val prisonerResult = prisonerSearchResponse.firstOrNull()
      ?: throw NoResultsReturnedException("No prisoner search results returned for ${dischargeToHospital.offenderNo}")

    if (!isCorrectLegalStatus(prisonerResult.legalStatus))
      throw ValidationException("Can not discharge prisoner with a legal status of ${prisonerResult.legalStatus}")

    val response = prisonApiGateway.dischargeToHospital(dischargeToHospital)

    restrictedPatientsRepository.save(
      RestrictivePatient(
        fromLocationId = dischargeToHospital.fromLocationId,
        hospitalLocationCode = response.restrictivePatient.dischargedHospital.agencyId,
        supportingPrisonId = response.restrictivePatient.supportingPrison.agencyId,
        dischargeTime = dischargeToHospital.dischargeTime,
        commentText = dischargeToHospital.commentText
      )
    )

    return response
  }

  private fun isCorrectLegalStatus(legalStatus: LegalStatus?): Boolean =
    LegalStatus.permissibleLegalStatusesForDischargingPrisonersToHospital().contains(legalStatus)
}
