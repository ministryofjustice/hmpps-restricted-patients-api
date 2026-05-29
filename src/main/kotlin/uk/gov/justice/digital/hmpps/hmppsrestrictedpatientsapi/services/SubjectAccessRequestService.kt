package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiQueryService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientContent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(
  private val repository: RestrictedPatientsRepository,
  private val prisonApiQueryService: PrisonApiQueryService,
) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?) = repository.findByIdOrNull(prn)?.let {
    val hospital = prisonApiQueryService.getAgency(it.hospitalLocationCode)
    val fromLocation = prisonApiQueryService.getAgency(it.fromLocationId)

    HmppsSubjectAccessRequestContent(
      content = RestrictedPatientContent(
        prisonerNumber = it.prisonerNumber,
        supportingPrisonId = it.supportingPrisonId,
        hospitalLocationCode = it.hospitalLocationCode,
        hospitalLocationDescription = hospital?.description ?: it.hospitalLocationCode,
        dischargeTime = it.dischargeTime,
        commentText = it.commentText,
        fromLocationName = fromLocation?.description ?: it.fromLocationId,
        createdDate = it.createDateTime,
        createdUserSurname = it.createUserId?.let { username -> surnameFor(username) },
        modifiedDate = it.modifyDateTime,
        modifiedUserSurname = it.modifyUserId?.let { username -> surnameFor(username) },
      ),
    )
  }

  private fun surnameFor(username: String): String? = prisonApiQueryService.getUser(username)?.lastName
}
