package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request

data class SupportingPrisonRequest(
  val offenderNo: String,
  val supportingPrisonId: String,
)
