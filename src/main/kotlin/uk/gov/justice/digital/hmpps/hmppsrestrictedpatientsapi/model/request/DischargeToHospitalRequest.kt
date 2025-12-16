package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class DischargeToHospitalRequest(
  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA", required = true)
  val offenderNo: String,
  val commentText: String? = null,
  @Schema(description = "From location id", example = "MDI", required = true)
  val fromLocationId: String,
  @Schema(description = "Hospital location code", example = "HAZLWD", required = true)
  val hospitalLocationCode: String,
  val supportingPrisonId: String? = null,
  val dischargeTime: LocalDateTime? = null,
)
