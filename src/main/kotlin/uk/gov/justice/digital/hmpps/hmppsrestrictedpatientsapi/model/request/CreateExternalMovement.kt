package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class CreateExternalMovement(
  val bookingId: Long,
  val fromAgencyId: String,
  val toAgencyId: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val movementTime: LocalDateTime,
  val movementType: String,
  val movementReason: String,
  val directionCode: String
)
