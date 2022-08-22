package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import java.time.LocalDate

data class DischargeToHospitalResponse(
  val restrictivePatient: RestrictivePatient
)

data class MovementResponse(
  val fromAgency: String?,
  val movementDate: String?,
  val movementTime: String?,
  val movementType: String?,
  val commentText: String?,
)

data class RestrictivePatient(
  val supportingPrison: Agency? = null,
  val dischargedHospital: Agency? = null,
  val dischargeDate: LocalDate? = null,
  val dischargeDetails: String? = null
)

data class OffenderBookingResponse(val bookingId: Long, val offenderNo: String, val activeFlag: ActiveFlag) {
  val active: Boolean
    get() = activeFlag == ActiveFlag.Y
}

enum class ActiveFlag {
  Y, N
}
