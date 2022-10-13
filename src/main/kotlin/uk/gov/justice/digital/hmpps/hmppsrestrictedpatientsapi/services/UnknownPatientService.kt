package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class UnknownPatientService(
  private val agencyFinder: AgencyFinder,
  private val prisonApiGateway: PrisonApiGateway,
  private val restrictedPatientsService: RestrictedPatientsService
) {

  fun migrateInUnknownPatients(patients: List<String>, dryRun: Boolean): List<UnknownPatientResult> =
    patients.drop(1)
      .map { rawPatient -> migrateInUnknownPatient(rawPatient, dryRun) }

  private fun migrateInUnknownPatient(rawPatient: String, dryRun: Boolean): UnknownPatientResult =
    runCatching {
      parsePatient(rawPatient)
        .takeIf { dryRun }
        ?.let { UnknownPatientResult(it.mhcsReference, null, true) }
        ?: let { migrateInPatient(parsePatient(rawPatient)) }
    }.getOrElse { ex ->
      when (ex) {
        is MigrateUnknownPatientException -> UnknownPatientResult(ex.mhcsReference, ex.offenderNumber, false, ex.message)
        else -> UnknownPatientResult(rawPatient, null, false, ex.message)
      }
    }

  fun parsePatient(patient: String): UnknownPatient =
    CSVParser.parse(patient, CSVFormat.DEFAULT).first().let {
      UnknownPatient(
        mhcsReference = it.mhcsReference(),
        surname = it.surname(),
        firstName = it.firstName(),
        middleNames = it.middleNames(),
        gender = it.gender(),
        dateOfBirth = it.dateOfBirth(),
        prisonCode = it.prisonName(),
        hospitalCode = it.hospitalName(),
        hospitalOrderDate = it.hospitalMoveDate(),
      )
    }

  private fun migrateInPatient(unknownPatient: UnknownPatient): UnknownPatientResult =
    unknownPatient.createPrisoner().offenderNo
      .also { offenderNumber -> unknownPatient.dischargeToHospital(offenderNumber) }
      .let { offenderNumber ->
        UnknownPatientResult(mhcsReference = unknownPatient.mhcsReference, offenderNumber = offenderNumber, success = true)
      }

  private fun UnknownPatient.createPrisoner() =
    runCatching { prisonApiGateway.createPrisoner(surname, firstName, middleNames, gender, dateOfBirth) }
      .getOrElse { throw MigrateUnknownPatientException(mhcsReference, "Create prisoner failed due to: ${it.message}") }

  private fun UnknownPatient.dischargeToHospital(offenderNumber: String) =
    runCatching {
      DischargeToHospitalRequest(
        offenderNumber,
        "Historical hospital release added to NOMIS for addition to Restricted Patients",
        prisonCode,
        hospitalCode,
        prisonCode,
        hospitalOrderDate.atStartOfDay(),
      )
        .let { restrictedPatientsService.dischargeToHospital(it) }
    }
      .getOrElse { throw MigrateUnknownPatientException(mhcsReference, "Discharge to hospital failed due to: ${it.message}", offenderNumber) }

  private fun CSVRecord.mhcsReference() = this[0].ifEmpty { throw IllegalArgumentException("MHCS Reference must not be blank") }
  private fun CSVRecord.surname() = this[1]
  private fun CSVRecord.firstName() = this[2].split(" ").first()
  private fun CSVRecord.middleNames() = this[2].split(" ").drop(1).joinToString(" ")
  private fun CSVRecord.gender() = this[3].takeIf { listOf("M", "F").contains(it) } ?: throw MigrateUnknownPatientException(this[0], "Gender of ${this[3]} should be M or F")
  private fun CSVRecord.dateOfBirth() = runCatching { LocalDate.parse(this[4], DateTimeFormatter.ISO_DATE) }.getOrElse { throw MigrateUnknownPatientException(this[0], "Date of birth ${this[4]} invalid") }
  private fun CSVRecord.prisonName() = agencyFinder.findPrisonCode(this[8]) ?: throw MigrateUnknownPatientException(this[0], "Could not find prison ${this[8]}")
  private fun CSVRecord.hospitalName() = agencyFinder.findHospitalCode(this[16]) ?: throw MigrateUnknownPatientException(this[0], "Could not find hospital ${this[16]}")
  private fun CSVRecord.hospitalMoveDate() = kotlin.runCatching { LocalDate.parse(this[17], DateTimeFormatter.ISO_DATE) }.getOrElse { throw MigrateUnknownPatientException(this[0], "Date of hospital order ${this[17]} invalid") }
}

data class UnknownPatient(
  val mhcsReference: String,
  val surname: String,
  val firstName: String,
  val middleNames: String?,
  val gender: String,
  val dateOfBirth: LocalDate,
  val prisonCode: String,
  val hospitalCode: String,
  val hospitalOrderDate: LocalDate
)

data class UnknownPatientResult(
  val mhcsReference: String?,
  val offenderNumber: String? = null,
  val success: Boolean,
  val errorMessage: String? = null
)

class MigrateUnknownPatientException(val mhcsReference: String, errorMessage: String, val offenderNumber: String? = null) : RuntimeException(errorMessage)
