package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

data class MigrateInRequest(
  @Schema(description = "Offender number", example = "A1234AA", required = true)
  val offenderNo: String,
  @Schema(description = "Hospital location code", example = "MDI", required = true)
  val hospitalLocationCode: String,
)
