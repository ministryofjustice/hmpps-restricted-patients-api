package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Restricted patient")
data class RestrictedPatientDto(
  @Schema(description = "Then NOMS Id of the restricted patient", example = "G1072GT")
  val prisonerNumber: String,
  @Schema(description = "Previous location id", example = "MDI")
  val fromLocation: Agency?,
  @Schema(description = "Hospital location code, this indicates the current location of a prisoner", example = "HAZLWD")
  val hospitalLocation: Agency?,
  @Schema(description = "Prison where the offender is supported by POM", example = "LEI")
  val supportingPrison: Agency? = null,
  @Schema(description = "Date and time the prisoner was discharged to hospital")
  val dischargeTime: LocalDateTime,
  @Schema(description = "Useful comments")
  val commentText: String? = null,
  @Schema(description = "Date time the record was created")
  var createDateTime: LocalDateTime? = null,
  @Schema(description = "The user id of the person who created the record")
  var createUserId: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Agency(
  val agencyId: String,
  val description: String? = null,
  val longDescription: String? = null,
  val agencyType: String? = null,
  val active: Boolean? = null
)
