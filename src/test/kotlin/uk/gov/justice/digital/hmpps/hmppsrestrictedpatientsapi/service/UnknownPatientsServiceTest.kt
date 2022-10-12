package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.AgencyFinder
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.MigrateUnknownPatientException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientService
import java.time.LocalDate

class UnknownPatientsServiceTest {

  private val agencyFinder = mock<AgencyFinder>()
  private val service = UnknownPatientService(agencyFinder)

  private val testFile = listOf(
    """FILE_REFERENCE,FAMILY_NAME,FIRST_NAMES,Gender,DOB,Date of Sentence,Court sentenced at,Reason for reception,Prison received into,Under 21 at point of sentence?,Sentence type,Offence (list all current),CJA/Code,Sentence length,Offence to attach to sentence (most serious),AUTHORITY_FOR_DETENTION_DESCRIPTION,CURRENT_ESTABLISHMENT_DESCRIPTION,DATE_OF_HOSPITAL_ORDER""",
    """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01""",
    """,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01""",
    """3/6170,O'Brien,Steven John M,M,1965-02-33,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01""",
    """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-33""",
    """3/6170,O'Brien,Steven John M,Y,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01""",
    """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Unknown,2011-09-01""",
    """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,Unknown,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01""",
  )

  @BeforeEach
  fun setUp() {
    whenever(agencyFinder.findHospitalCode("Broadmoor Hospital")).thenReturn("BROADM")
    whenever(agencyFinder.findHospitalCode("Unknown")).thenReturn(null)
    whenever(agencyFinder.findPrisonCode("HMP High Down")).thenReturn("HOI")
    whenever(agencyFinder.findHospitalCode("Unknown")).thenReturn(null)
  }

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
        { assertThat(patient.prisonCode).isEqualTo("HOI") },
        { assertThat(patient.hospitalCode).isEqualTo("BROADM") },
        { assertThat(patient.hospitalOrderDate).isEqualTo(LocalDate.of(2011, 9, 1)) },
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

    @Test
    fun `hospital must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testFile[6])
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }

    @Test
    fun `prison must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testFile[7])
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
        UnknownPatientResult("3/6170", null, false, "Date of birth 1965-02-33 invalid"),
        UnknownPatientResult("3/6170", null, false, "Gender of Y should be M or F"),
      )
    }
  }
}
