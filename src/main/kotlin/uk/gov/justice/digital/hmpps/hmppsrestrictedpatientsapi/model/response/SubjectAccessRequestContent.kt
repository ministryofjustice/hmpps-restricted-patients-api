package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class SubjectAccessRequestContent(
  @Schema(description = "The content of the subject access request response")
  val content: RestrictedPatientContent,
)

data class RestrictedPatientContent(
  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  val prisonerNumber: String,
  @Schema(
    description = "Prison where the offender is supported by POM.  This is from AGENCY_LOCATIONS.AGY_LOC_ID in NOMIS.",
    example = "LEI",
  )
  val supportingPrisonId: String,
  @Schema(
    description = "Hospital location code, this indicates the current location of a prisoner. This is from AGENCY_LOCATIONS.AGY_LOC_ID in NOMIS.",
    example = "HAZLWD",
  )
  val hospitalLocationCode: String,
  @Schema(description = "Date and time the prisoner was discharged to hospital")
  val dischargeTime: LocalDateTime,
  @Schema(description = "Useful comments")
  val commentText: String?,
)
