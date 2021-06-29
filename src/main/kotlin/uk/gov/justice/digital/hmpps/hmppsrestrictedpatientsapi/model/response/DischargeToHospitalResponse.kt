package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import java.time.LocalDate

data class DischargeToHospitalResponse(
  val offenderNo: String,
  val bookingId: Long,
  val offenderId: Long,
  val rootOffenderId: Long,
  val assignedLivingUnit: AssignedLivingUnit,
  val inOutStatus: String,
  val status: String,
  val statusReason: String,
  val lastMovementTypeCode: String,
  val lastMovementReasonCode: String,
  val restrictivePatient: RestrictivePatient
)

data class RestrictivePatient(
  val supportingPrison: SupportingPrison,
  val dischargedHospital: DischargedHospital,
  val dischargeDate: LocalDate,
  val dischargeDetails: String
)

data class SupportingPrison(
  val agencyId: String,
  val description: String,
  val longDescription: String,
  val agencyType: String,
  val active: Boolean
)

data class DischargedHospital(
  val agencyId: String,
  val description: String,
  val longDescription: String,
  val agencyType: String,
  val active: Boolean
)

data class AssignedLivingUnit(
  val agencyId: String,
  val agencyName: String
)
