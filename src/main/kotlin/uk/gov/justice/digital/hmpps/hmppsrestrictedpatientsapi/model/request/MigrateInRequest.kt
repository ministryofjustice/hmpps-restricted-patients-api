package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request

data class MigrateInRequest(
  val offenderNo: String,
  val hospitalLocationCode: String,
)
