package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import java.time.LocalDate

data class PrisonerResult(
  val prisonerNumber: String,
  val bookingId: Long,
  val conditionalReleaseDate: LocalDate?,
  val indeterminateSentence: Boolean?, // will be null if no sentences set
)
