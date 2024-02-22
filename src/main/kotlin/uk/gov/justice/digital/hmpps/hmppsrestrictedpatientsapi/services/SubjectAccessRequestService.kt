package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientContent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(private val repository: RestrictedPatientsRepository) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?) =
    repository.findByIdOrNull(prn)?.let {
      HmppsSubjectAccessRequestContent(
        content = RestrictedPatientContent(
          prisonerNumber = it.prisonerNumber,
          supportingPrisonId = it.supportingPrisonId,
          hospitalLocationCode = it.hospitalLocationCode,
          dischargeTime = it.dischargeTime,
          commentText = it.commentText,
        ),
      )
    }
}
