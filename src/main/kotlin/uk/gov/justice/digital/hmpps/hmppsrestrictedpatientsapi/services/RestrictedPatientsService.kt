package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions.NoResultsReturnedException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.DischargeToHospitalResponse
import javax.validation.ValidationException

@Service
class RestrictedPatientsService(
  private val prisonApiGateway: PrisonApiGateway,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway
) {
  fun dischargeToHospital(dischargeToHospital: DischargeToHospitalRequest): DischargeToHospitalResponse {

    val prisonerSearchResponse = prisonerSearchApiGateway.searchByPrisonNumber(dischargeToHospital.offenderNo)
    val prisonerResult = prisonerSearchResponse.firstOrNull()
      ?: throw NoResultsReturnedException("No prisoner search results returned for ${dischargeToHospital.offenderNo}")

    if (!isCorrectLegalStatus(prisonerResult.legalStatus))
      throw ValidationException("Can not discharge prisoner with a legal status of ${prisonerResult.legalStatus}")

    return prisonApiGateway.dischargeToHospital(dischargeToHospital)
  }

  private fun isCorrectLegalStatus(legalStatus: LegalStatus?): Boolean = legalStatus != null && listOf(
    LegalStatus.INDETERMINATE_SENTENCE,
    LegalStatus.RECALL,
    LegalStatus.SENTENCED,
    LegalStatus.CONVICTED_UNSENTENCED,
    LegalStatus.IMMIGRATION_DETAINEE
  ).contains(legalStatus)
}
