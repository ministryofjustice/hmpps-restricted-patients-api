package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import java.time.LocalDateTime

data class RestrictedPatientDto(
  val id: Long,
  val prisonerNumber: String,
  val fromLocationId: String,
  val hospitalLocationCode: String,
  val supportingPrisonId: String? = null,
  val dischargeTime: LocalDateTime,
  val commentText: String? = null,
  val active: Boolean? = true,
  var createDateTime: LocalDateTime? = null,
  var createUserId: String? = null
)
