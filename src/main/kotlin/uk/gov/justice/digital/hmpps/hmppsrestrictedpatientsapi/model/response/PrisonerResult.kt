package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus

data class PrisonerResult(
  val prisonerNumber: String,
  val bookingId: Long,
  val legalStatus: LegalStatus
)
