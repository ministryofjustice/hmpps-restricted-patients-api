package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request

import java.time.LocalDateTime

data class CreateExternalMovement(
  val bookingId: Long,
  val fromAgencyId: String,
  val toAgencyId: String,
  val movementTime: LocalDateTime,
  val movementType: String,
  val movementReason: String,
  val directionCode: String
)