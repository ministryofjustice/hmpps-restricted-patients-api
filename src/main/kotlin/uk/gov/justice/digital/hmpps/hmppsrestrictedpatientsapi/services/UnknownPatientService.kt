package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.CaseNoteApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.CaseNoteRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.CommunityApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class UnknownPatientService(
  private val agencyFinder: AgencyFinder,
  private val prisonApiGateway: PrisonApiGateway,
  private val restrictedPatientsService: RestrictedPatientsService,
  private val communityApiGateway: CommunityApiGateway,
  private val caseNotesApiGateway: CaseNoteApiGateway,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway,
) {

  fun migrateInUnknownPatient(rawPatient: String, dryRun: Boolean = false): UnknownPatientResult =
    runCatching {
      parsePatient(rawPatient)
        .let {
          if (!dryRun) {
            migrateInPatient(it)
          } else {
            UnknownPatientResult(it.mhcsReference, null, true)
          }
        }
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
        croNumber = it.croNumber(),
        pncNumber = it.pncNumber(),
        crn = it.crn(),
      )
    }

  private fun migrateInPatient(unknownPatient: UnknownPatient): UnknownPatientResult =
    with(unknownPatient) {
      createPrisoner().offenderNo
        .also { offenderNumber ->
          dischargeToHospital(offenderNumber)
          createCaseNote(offenderNumber)
          updateNomsNumber(offenderNumber)
        }
        .let { offenderNumber ->
          UnknownPatientResult(mhcsReference = unknownPatient.mhcsReference, offenderNumber = offenderNumber, success = true)
        }
    }

  private fun UnknownPatient.createPrisoner(): InmateDetail =
    runCatching { prisonApiGateway.createPrisoner(surname, firstName, middleNames, gender, dateOfBirth, croNumber, pncNumber) }
      .getOrElse { handleApiError("Create prisoner failed due to", it) }

  private fun UnknownPatient.dischargeToHospital(offenderNumber: String) =
    runCatching {
      RestrictedPatient(
        offenderNumber,
        prisonCode,
        hospitalCode,
        prisonCode,
        hospitalOrderDate.atStartOfDay(),
        "Historical hospital release added to NOMIS for addition to Restricted Patients",
      )
        .let { restrictedPatientsService.addRestrictedPatient(it, noEventPropagation = true) }
        .also { prisonerSearchApiGateway.refreshPrisonerIndex(offenderNumber) }
    }
      .getOrElse { handleApiError("Discharge to hospital failed due to", it, offenderNumber) }

  private fun UnknownPatient.updateNomsNumber(offenderNumber: String): Unit? =
    runCatching {
      if (crn.isNullOrEmpty()) return null
      communityApiGateway.updateNomsNumber(crn, offenderNumber)
    }
      .getOrElse { handleApiError("Update community NOMS number failed due to", it, offenderNumber) }

  private fun UnknownPatient.createCaseNote(offenderNumber: String): Unit =
    runCatching {
      caseNotesApiGateway.createCaseNote(
        CaseNoteRequest(
          offenderNumber = offenderNumber,
          type = "MIGRATION",
          subType = "OBS_GEN",
          locationId = prisonCode,
          occurrenceDateTime = LocalDateTime.now(),
          text = "Automatic admission and release to psychiatric hospital for Restricted Patients",
        ),
      )
    }
      .getOrElse { handleApiError("Create case note failed due to", it, offenderNumber) }

  private fun UnknownPatient.handleApiError(errorType: String, it: Throwable, offenderNumber: String? = null): Nothing {
    when (it) {
      is WebClientResponseException -> "$errorType: ${it.statusText}, ${it.responseBodyAsString}".replace(Regex(", $"), "")
      else -> "$errorType: ${it.message}"
    }.let { errorMessage ->
      throw MigrateUnknownPatientException(mhcsReference, errorMessage, offenderNumber)
    }
  }

  private fun CSVRecord.mhcsReference() = this[0].ifEmpty { throw IllegalArgumentException("MHCS Reference must not be blank") }
  private fun CSVRecord.surname() = this[1].trim()
  private fun CSVRecord.firstName() = this[2].trim().split(" ").first()
  private fun CSVRecord.middleNames() = this[2].trim().split(" ").drop(1).joinToString(" ").takeIf { it.isNotEmpty() }
  private fun CSVRecord.gender() = this[3].takeIf { listOf("M", "F").contains(it) } ?: throw MigrateUnknownPatientException(this[0], "Gender of ${this[3]} should be M or F")
  private fun CSVRecord.dateOfBirth() = runCatching { LocalDate.parse(this[4], DateTimeFormatter.ISO_DATE) }.getOrElse { throw MigrateUnknownPatientException(this[0], "Date of birth ${this[4]} invalid") }
  private fun CSVRecord.prisonName() = agencyFinder.findPrisonCode(this[8]) ?: throw MigrateUnknownPatientException(this[0], "Could not find prison ${this[8]}")
  private fun CSVRecord.hospitalName() = agencyFinder.findHospitalCode(this[16]) ?: throw MigrateUnknownPatientException(this[0], "Could not find hospital ${this[16]}")
  private fun CSVRecord.hospitalMoveDate() = kotlin.runCatching { LocalDate.parse(this[17], DateTimeFormatter.ISO_DATE) }.getOrElse { throw MigrateUnknownPatientException(this[0], "Date of hospital order ${this[17]} invalid") }
  private fun CSVRecord.croNumber() = this[18].takeIf { it.isNotEmpty() }
  private fun CSVRecord.pncNumber() = this[19].takeIf { it.isNotEmpty() }
  private fun CSVRecord.crn() = this[20].takeIf { it.isNotEmpty() }
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
  val hospitalOrderDate: LocalDate,
  val croNumber: String?,
  val pncNumber: String?,
  val crn: String?,
)

data class UnknownPatientResult(
  val mhcsReference: String?,
  val offenderNumber: String? = null,
  val success: Boolean,
  val errorMessage: String? = null,
)

class MigrateUnknownPatientException(val mhcsReference: String, errorMessage: String, val offenderNumber: String? = null) : RuntimeException(errorMessage)
