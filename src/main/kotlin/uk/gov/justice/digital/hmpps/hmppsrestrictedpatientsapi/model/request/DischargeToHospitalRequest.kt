package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request

data class DischargeToHospitalRequest(
  val offenderNo: String,
  val commentText: String? = null,
  val fromLocationId: String,
  val hospitalLocationCode: String,
  val supportingPrisonId: String? = null
)
