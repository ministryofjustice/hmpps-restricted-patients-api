package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import java.time.LocalDate

data class MovementResponse(
  val fromAgency: String?,
  val movementDate: String?,
  val movementTime: String?,
  val movementType: String?,
  val commentText: String?,
  val toAgency: String?,
  val movementReasonCode: String?,
)

data class RestrictedPatient(
  val supportingPrison: Agency? = null,
  val dischargedHospital: Agency? = null,
  val dischargeDate: LocalDate? = null,
  val dischargeDetails: String? = null,
)

data class OffenderBookingResponse(val bookingId: Long, val offenderNo: String, val activeFlag: Boolean)
