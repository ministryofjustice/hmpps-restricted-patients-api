package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientContent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.LocalDate

@Service
class SubjectAccessRequestService(private val repository: RestrictedPatientsRepository) {
  fun getContentFor(prisonerNumber: String, fromDate: LocalDate?, toDate: LocalDate?) =
    repository.findByIdOrNull(prisonerNumber)?.let {
      SubjectAccessRequestContent(
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
