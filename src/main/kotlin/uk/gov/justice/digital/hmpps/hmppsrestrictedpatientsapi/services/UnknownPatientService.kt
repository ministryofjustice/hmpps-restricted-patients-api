package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class UnknownPatientService(private val agencyFinder: AgencyFinder, private val prisonApiGateway: PrisonApiGateway) {

  fun migrateInUnknownPatients(patients: List<String>): List<UnknownPatientResult> =
    patients.drop(1)
      .map { rawPatient -> migrateInUnknownPatient(rawPatient) }

  private fun migrateInUnknownPatient(rawPatient: String): UnknownPatientResult =
    runCatching {
      migrateInPatient(parsePatient(rawPatient))
    }.getOrElse { ex ->
      when (ex) {
        is MigrateUnknownPatientException -> UnknownPatientResult(ex.mhcsReference, null, false, ex.message)
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
    unknownPatient.createPrisoner()
      // TODO SDI-357 discharge-to-hospital and migrate-in-restricted-patient
      .let { offenderNumber ->
        UnknownPatientResult(mhcsReference = unknownPatient.mhcsReference, offenderNumber = offenderNumber, success = true)
      }

  private fun UnknownPatient.createPrisoner() =
    runCatching { prisonApiGateway.createPrisoner(surname, firstName, middleNames, gender, dateOfBirth) }
      .getOrElse { throw MigrateUnknownPatientException(mhcsReference, "Create prisoner failed due to: ${it.message}") }

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

class MigrateUnknownPatientException(val mhcsReference: String, errorMessage: String) : RuntimeException(errorMessage)
