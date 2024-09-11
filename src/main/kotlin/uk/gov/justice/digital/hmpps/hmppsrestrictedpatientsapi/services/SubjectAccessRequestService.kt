package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientContent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(
  private val repository: RestrictedPatientsRepository,
  private val prisonApiApplicationGateway: PrisonApiGateway,
) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?) =
    repository.findByIdOrNull(prn)?.let {
      val prison = prisonApiApplicationGateway.getAgency(it.supportingPrisonId)
      val hospital = prisonApiApplicationGateway.getAgency(it.hospitalLocationCode)

      HmppsSubjectAccessRequestContent(
        content = RestrictedPatientContent(
          prisonerNumber = it.prisonerNumber,
          supportingPrisonId = it.supportingPrisonId,
          supportingPrisonDescription = prison?.description ?: it.supportingPrisonId,
          hospitalLocationCode = it.hospitalLocationCode,
          hospitalLocationDescription = hospital?.description ?: it.hospitalLocationCode,
          dischargeTime = it.dischargeTime,
          commentText = it.commentText,
        ),
      )
    }
}
