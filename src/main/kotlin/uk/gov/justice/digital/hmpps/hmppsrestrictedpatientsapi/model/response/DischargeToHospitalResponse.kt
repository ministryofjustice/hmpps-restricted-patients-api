package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import java.time.LocalDate

data class DischargeToHospitalResponse(
  val restrictivePatient: RestrictivePatient
)

data class RestrictivePatient(
  val supportingPrison: Agency? = null,
  val dischargedHospital: Agency? = null,
  val dischargeDate: LocalDate? = null,
  val dischargeDetails: String? = null
)
