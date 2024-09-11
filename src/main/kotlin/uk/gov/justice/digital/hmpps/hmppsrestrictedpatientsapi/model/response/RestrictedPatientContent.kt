package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(NON_NULL)
data class RestrictedPatientContent(
  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  val prisonerNumber: String,
  @Schema(
    description = "Prison ID where the offender is supported by POM.  This is from AGENCY_LOCATIONS.AGY_LOC_ID in NOMIS.",
    example = "LEI",
  )
  val supportingPrisonId: String,
  @Schema(
    description = "Prison where the offender is supported by POM.",
    example = "LEI",
  )
  val supportingPrisonDescription: String?,
  @Schema(
    description = "Hospital location code, this indicates the current location of a prisoner. This is from AGENCY_LOCATIONS.AGY_LOC_ID in NOMIS.",
    example = "HAZLWD",
  )
  val hospitalLocationCode: String,
  @Schema(
    description = "Hospital location name, this indicates the current location of a prisoner.",
    example = "Hazelwood House",
  )
  val hospitalLocationDescription: String?,
  @Schema(description = "Date and time the prisoner was discharged to hospital")
  val dischargeTime: LocalDateTime,
  @Schema(description = "Useful comments")
  val commentText: String?,
)
