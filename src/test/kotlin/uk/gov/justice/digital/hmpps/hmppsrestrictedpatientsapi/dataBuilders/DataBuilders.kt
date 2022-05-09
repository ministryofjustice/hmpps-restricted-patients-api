package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders

import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.PrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
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
  fromLocationId: String = "MDI",
  hospitalLocationCode: String = "HAZLWD",
  supportingPrisonId: String? = null
) = DischargeToHospitalRequest(
  offenderNo,
  commentText,
  fromLocationId,
  hospitalLocationCode,
  supportingPrisonId
)

fun makeRestrictedPatientDto(
  fromLocationId: Agency = PRISON,
  supportingPrisonId: Agency = PRISON,
  prisonerNumber: String = "A12345",
  hospitalLocationCode: Agency = HOSPITAL,
  commentText: String = "test",
  dischargeTime: LocalDateTime = LocalDateTime.parse("2020-10-10T20:00:01"),
  createDateTime: LocalDateTime = LocalDateTime.parse("2020-10-10T20:00:01"),
  createUserId: String = "user"
): RestrictedPatientDto = RestrictedPatientDto(
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

fun makePrisonerResult(
  prisonerNumber: String = "A12345",
  bookingId: Long = 1L
): PrisonerResult = PrisonerResult(prisonerNumber = prisonerNumber, bookingId = bookingId)

fun makePrisonerReceiveEvent(prisonerNumber: String) =
  """
    {
        "Type": "Notification",
        "MessageId": "5b90ee7d-67bc-5959-a4d8-b7d420180853",
        "Message": "{\"eventType\":\"prison-offender-events.prisoner.received\",\"additionalInformation\":{\"nomsNumber\":\"$prisonerNumber\",\"reason\":\"ADMISSION\",\"probableCause\":\"RECALL\",\"source\":\"PRISON\",\"details\":\"ACTIVE IN:ADM-24\",\"currentLocation\":\"IN_PRISON\",\"prisonId\":\"LEI\",\"currentPrisonStatus\":\"UNDER_PRISON_CARE\"},\"version\":1,\"occurredAt\":\"2021-09-01T10:16:57.435579+01:00\",\"publishedAt\":\"2021-09-01T10:18:28.69751046+01:00\",\"description\":\"A prisoner has been received into prison\"}",
        "Timestamp": "2021-09-01T09:18:28.725Z",
        "MessageAttributes": {
            "eventType": {
                "Type": "String",
                "Value": "prison-offender-events.prisoner.received"
            }
        }
    }
  """.trimIndent()
