package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders

import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.DischargeToHospitalResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.PrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictivePatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.ExternalPrisonerMovementMessage
import java.time.LocalDate
import java.time.LocalDateTime

val PRISON =
  Agency(agencyId = "MDI", agencyType = "INST", active = true, description = "Moorland", longDescription = "Moorland")
val HOSPITAL = Agency(
  agencyId = "HAZLWD",
  agencyType = "HSHOSP",
  active = true,
  description = "Hazelwood House",
  longDescription = "Hazelwood House"
)

fun makeDischargeRequest(
  offenderNo: String = "A12345",
  commentText: String = "test",
  dischargeTime: LocalDateTime = LocalDateTime.parse("2020-10-10T20:00:01"),
  fromLocationId: String = "MDI",
  hospitalLocationCode: String = "HAZLWD",
  supportingPrisonId: String = "MDI"
) = DischargeToHospitalRequest(
  offenderNo,
  commentText,
  dischargeTime,
  fromLocationId,
  hospitalLocationCode,
  supportingPrisonId
)

fun makeRestrictedPatientDto(
  id: Long = 1,
  fromLocationId: Agency = PRISON,
  supportingPrisonId: Agency = PRISON,
  prisonerNumber: String = "A12345",
  hospitalLocationCode: Agency = HOSPITAL,
  commentText: String = "test",
  dischargeTime: LocalDateTime = LocalDateTime.parse("2020-10-10T20:00:01"),
  createDateTime: LocalDateTime = LocalDateTime.parse("2020-10-10T20:00:01"),
  createUserId: String = "user"
): RestrictedPatientDto = RestrictedPatientDto(
  id,
  prisonerNumber,
  fromLocationId,
  hospitalLocationCode,
  supportingPrisonId,
  dischargeTime,
  commentText,
  createDateTime,
  createUserId
)

fun makeRestrictedPatient(
  id: Long = 1,
  fromLocationId: String = "MDI",
  supportingPrisonId: String = "MDI",
  prisonerNumber: String = "A12345",
  hospitalLocationCode: String = "HAZLWD",
  commentText: String = "test",
  dischargeTime: LocalDateTime = LocalDateTime.parse("2020-10-10T20:00:01"),
  createDateTime: LocalDateTime = LocalDateTime.parse("2020-10-10T20:00:01"),
  createUserId: String = "ITAG_USER"
): RestrictedPatient {
  val patient = RestrictedPatient(
    id,
    prisonerNumber,
    fromLocationId,
    hospitalLocationCode,
    supportingPrisonId,
    dischargeTime,
    commentText
  )
  patient.createDateTime = createDateTime
  patient.createUserId = createUserId
  return patient
}

fun makeDischargeToHospitalResponse(
  dischargeDate: LocalDate = LocalDate.now(),
  dischargeDetails: String = "test"
): DischargeToHospitalResponse = DischargeToHospitalResponse(
  restrictivePatient = RestrictivePatient(
    supportingPrison = PRISON,
    dischargedHospital = HOSPITAL,
    dischargeDate = dischargeDate,
    dischargeDetails = dischargeDetails
  )
)

fun makePrisonerResult(
  prisonerNumber: String = "A12345",
  legalStatus: LegalStatus = LegalStatus.SENTENCED,
  bookingId: Long = 1L
): PrisonerResult = PrisonerResult(prisonerNumber = prisonerNumber, legalStatus = legalStatus, bookingId = bookingId)

fun makeExternalMovementEventAsJson(offenderNumber: String) =
  """
{
  "MessageId": "message2",
  "Type": "Notification",
  "Timestamp": "2019-11-11T11:11:11.111111Z",
  "Message": "{\"eventType\": \"EXTERNAL_MOVEMENT_RECORD-INSERTED\", \"bookingId\": 100001, \"movementSeq\": 3, \"offenderIdDisplay\": \"$offenderNumber\", \"movementDateTime\": \"2020-02-29T12:34:56\", \"movementType\": \"ADM\", \"movementReasonCode\": \"ADM\", \"directionCode\": \"IN\", \"escortCode\": \"POL\", \"fromAgencyLocationId\": \"CRTTRN\", \"toAgencyLocationId\": \"MDI\"}",
  "TopicArn": "arn:aws:sns:eu-west-2:000000000000:offender_events",
  "MessageAttributes": {
    "eventType": {
      "Type": "String",
      "Value": "EXTERNAL_MOVEMENT_RECORD-INSERTED"
    },
    "contentType": {
      "Type": "String",
      "Value": "text/plain;charset=UTF-8"
    }
  }
}

  """.trimIndent()

fun makeExternalPrisonerMovementMessage(
  bookingId: Long = 1L,
  movementSeq: Long = 2L,
  offenderIdDisplay: String = "A12345",
  fromAgencyLocationId: String = "OUT",
  toAgencyLocationId: String = "MDI",
  directionCode: String = "IN",
  movementType: String = "ADM"
): ExternalPrisonerMovementMessage = ExternalPrisonerMovementMessage(
  bookingId,
  movementSeq,
  offenderIdDisplay,
  fromAgencyLocationId,
  toAgencyLocationId,
  directionCode,
  movementType
)
