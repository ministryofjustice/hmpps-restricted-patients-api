package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

data class MovementResponse(
  val fromAgency: String?,
  val movementDate: String?,
  val movementTime: String?,
  val movementType: String?,
  val commentText: String?,
  val toAgency: String?,
  val movementReasonCode: String?,
)

data class OffenderBookingResponse(val bookingId: Long, val offenderNo: String, val activeFlag: Boolean)
