package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel(description = "Restricted patient")
data class RestrictedPatientDto(
  @ApiModelProperty(value = "Id of the restricted patient record", example = "1")
  val id: Long,
  @ApiModelProperty(value = "Then NOMS Id of the restricted patient", example = "G1072GT")
  val prisonerNumber: String,
  @ApiModelProperty(value = "Previous location id", example = "MDI")
  val fromLocationId: String,
  @ApiModelProperty(value = "Hospital location code, this indicates the current location of a prisoner", example = "HAZLWD")
  val hospitalLocationCode: String,
  @ApiModelProperty(value = "Prison where the offender is support by POM", example = "LEI")
  val supportingPrisonId: String? = null,
  @ApiModelProperty(value = "Date and time the prisoner was discharged to hospital")
  val dischargeTime: LocalDateTime,
  @ApiModelProperty(value = "Useful comments")
  val commentText: String? = null,
  @ApiModelProperty(value = "Indicates whether the prisoner is still a restricted patient")
  val active: Boolean? = true,
  @ApiModelProperty(value = "Date time the record was created")
  var createDateTime: LocalDateTime? = null,
  @ApiModelProperty(value = "The user id of the person who created the record")
  var createUserId: String? = null
)
