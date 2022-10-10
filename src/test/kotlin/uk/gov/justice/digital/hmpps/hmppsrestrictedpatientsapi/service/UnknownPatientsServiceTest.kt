package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.MigrateUnknownPatientException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientService
import java.time.LocalDate

class UnknownPatientsServiceTest {

  private val service = UnknownPatientService()

  private val testFile = listOf(
    """FILE_REFERENCE,FAMILY_NAME,FIRST_NAMES,Gender,DOB,Date of Sentence,Court sentenced at,Reason for reception,Prison received into,Under 21 at point of sentence?,Sentence type,Offence (list all current),CJA/Code,Sentence length,Offence to attach to sentence (most serious),AUTHORITY_FOR_DETENTION_DESCRIPTION,CURRENT_ESTABLISHMENT_DESCRIPTION,DATE_OF_HOSPITAL_ORDER""",
    """3/6170,O'Brien,Steven John M,M,11/02/1965,1/12/2014,Wood Green Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Wounding,,Tariff - 5 years,Wounding ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,01 Dec 2014 00:00""",
    """,O'Brien,Steven John M,M,11/02/1965,1/12/2014,Wood Green Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Wounding,,Tariff - 5 years,Wounding ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,01 Dec 2014 00:00""",
    """3/6170,O'Brien,Steven John M,M,33/02/1965,1/12/2014,Wood Green Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Wounding,,Tariff - 5 years,Wounding ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,01 Dec 2014 00:00""",
    """3/6170,O'Brien,Steven John M,M,11/02/1965,1/12/2014,Wood Green Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Wounding,,Tariff - 5 years,Wounding ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,33 Dec 2014 00:00""",
    """3/6170,O'Brien,Steven John M,Y,11/02/1965,1/12/2014,Wood Green Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Wounding,,Tariff - 5 years,Wounding ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,01 Dec 2014 00:00""",
  )

  @Nested
  inner class ParsePatient {
    @Test
    fun `can load valid patient`() {
      val patient = service.parsePatient(testFile[1])

      assertAll(
        { assertThat(patient.mhcsReference).isEqualTo("3/6170") },
        { assertThat(patient.surname).isEqualTo("O'Brien") },
        { assertThat(patient.firstName).isEqualTo("Steven") },
        { assertThat(patient.middleNames).isEqualTo("John M") },
        { assertThat(patient.gender).isEqualTo("M") },
        { assertThat(patient.dateOfBirth).isEqualTo(LocalDate.of(1965, 2, 11)) },
        { assertThat(patient.prisonCode).isEqualTo("HMP High Down") },
        { assertThat(patient.hospitalCode).isEqualTo("Broadmoor Hospital") },
        { assertThat(patient.hospitalOrderDate).isEqualTo(LocalDate.of(2014, 12, 1)) },
      )
    }

    @Test
    fun `MHCS reference must be entered`() {
      assertThatThrownBy {
        service.parsePatient(testFile[2])
      }.isInstanceOf(IllegalArgumentException::class.java)
        .withFailMessage { "MHCS Reference must not be blank" }
    }

    @Test
    fun `date of birth must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testFile[3])
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }

    @Test
    fun `hospital date must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testFile[4])
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }

    @Test
    fun `gender must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testFile[5])
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }
  }

  @Nested
  inner class MigrateInUnknownPatients {
    @Test
    fun `can migrate multiple patients`() {
      val results = service.migrateInUnknownPatients(listOf(testFile[0], testFile[1], testFile[3], testFile[5]))

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", "TODO", true, null),
        UnknownPatientResult("3/6170", null, false, "Date of birth 33/02/1965 invalid"),
        UnknownPatientResult("3/6170", null, false, "Gender of Y should be M or F"),
      )
    }
  }
}
