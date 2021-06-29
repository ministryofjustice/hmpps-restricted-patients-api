package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders

import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.*
import java.time.LocalDate
import java.time.LocalDateTime

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

fun makeDischargeToHospitalResponse(
  offenderNo: String = "A12345",
  bookingId: Long = 1,
  offenderId: Long = 2,
  rootOffenderId: Long = 3,
  assignedLivingUnit: AssignedLivingUnit = AssignedLivingUnit(agencyId = "OUT", agencyName = "Outside"),
  inOutStatus: String = "OUT",
  status: String = "INACTIVE OUT",
  statusReason: String = "REL-HP",
  lastMovementTypeCode: String = "REL",
  lastMovementReasonCode: String = "HP",
  restrictivePatient: RestrictivePatient = RestrictivePatient(
    SupportingPrison("MDI", "Moorland (HMP & YOI)", "HMP & YOI Moorland Prison near Doncaster", "INST", true),
    DischargedHospital("HAZLWD", "Hazelwood House", "Hazelwood House", "HSHOSP", true),
    dischargeDate = LocalDate.now().atStartOfDay().toLocalDate(),
    dischargeDetails = "Psychiatric Hospital Discharge to Hazelwood House"
  ),
) = DischargeToHospitalResponse(
  offenderNo,
  bookingId,
  offenderId,
  rootOffenderId,
  assignedLivingUnit,
  inOutStatus,
  status,
  statusReason,
  lastMovementTypeCode,
  lastMovementReasonCode,
  restrictivePatient,
)
