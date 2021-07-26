package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders

import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.*
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
