package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.CommunityApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.CommunityDetail
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.AgencyFinder
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.MigrateUnknownPatientException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientService
import java.nio.charset.Charset
import java.time.LocalDate

class UnknownPatientsServiceTest {

  private val agencyFinder = mock<AgencyFinder>()
  private val prisonApiGateway = mock<PrisonApiGateway>()
  private val restrictedPatientService = mock<RestrictedPatientsService>()
  private val communityApiGateway = mock<CommunityApiGateway>()
  private val service = UnknownPatientService(agencyFinder, prisonApiGateway, restrictedPatientService, communityApiGateway)

  private val testFile = mapOf(
    "header" to """FILE_REFERENCE,FAMILY_NAME,FIRST_NAMES,Gender,DOB,Date of Sentence,Court sentenced at,Reason for reception,Prison received into,Under 21 at point of sentence?,Sentence type,Offence (list all current),CJA/Code,Sentence length,Offence to attach to sentence (most serious),AUTHORITY_FOR_DETENTION_DESCRIPTION,CURRENT_ESTABLISHMENT_DESCRIPTION,DATE_OF_HOSPITAL_ORDER""",
    "valid" to """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
    "no_middle_name" to """3/6170,O'Brien,Steven,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
    "missing_mhcs_ref" to """,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
    "invalid_dob" to """3/6170,O'Brien,Steven John M,M,1965-02-33,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
    "invalid_hospital_date" to """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-33,cro,pnc,crn""",
    "invalid_gender" to """3/6170,O'Brien,Steven John M,Y,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
    "invalid_hospital" to """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Unknown,2011-09-01,cro,pnc,crn""",
    "invalid_prison" to """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,Unknown,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
  )

  private fun testRecord(test: String) = testFile[test] ?: throw IllegalStateException("Cannot find test data $test")

  @BeforeEach
  fun setUp() {
    whenever(agencyFinder.findHospitalCode("Broadmoor Hospital")).thenReturn("BROADM")
    whenever(agencyFinder.findHospitalCode("Unknown")).thenReturn(null)
    whenever(agencyFinder.findPrisonCode("HMP High Down")).thenReturn("HOI")
    whenever(agencyFinder.findHospitalCode("Unknown")).thenReturn(null)
    whenever(communityApiGateway.updateNomsNumber(anyString(), anyString())).thenReturn(CommunityDetail("cro", "pnc"))
  }

  @Nested
  inner class ParsePatient {
    @Test
    fun `can load valid patient`() {
      val patient = service.parsePatient(testRecord("valid"))

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
        { assertThat(patient.croNumber).isEqualTo("cro") },
        { assertThat(patient.pncNumber).isEqualTo("pnc") },
        { assertThat(patient.crn).isEqualTo("crn") },
      )
    }

    @Test
    fun `handles no middle name`() {
      val patient = service.parsePatient(testRecord("no_middle_name"))

      assertThat(patient.middleNames).isEmpty()
    }

    @Test
    fun `MHCS reference must be entered`() {
      assertThatThrownBy {
        service.parsePatient(testRecord("missing_mhcs_ref"))
      }.isInstanceOf(IllegalArgumentException::class.java)
        .withFailMessage { "MHCS Reference must not be blank" }
    }

    @Test
    fun `date of birth must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testRecord("invalid_dob"))
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }

    @Test
    fun `hospital date must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testRecord("invalid_hospital_date"))
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }

    @Test
    fun `gender must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testRecord("invalid_gender"))
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }

    @Test
    fun `hospital must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testRecord("invalid_hospital"))
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }

    @Test
    fun `prison must be valid`() {
      assertThatThrownBy {
        service.parsePatient(testRecord("invalid_prison"))
      }.isInstanceOf(MigrateUnknownPatientException::class.java)
    }
  }

  @Nested
  inner class MigrateInUnknownPatients {

    @BeforeEach
    fun setUp() {
      whenever(prisonApiGateway.createPrisoner(anyString(), anyString(), anyString(), anyString(), any(), anyOrNull(), anyOrNull()))
        .thenReturn(InmateDetail("A1234AA"))
      whenever(prisonApiGateway.dischargeToHospital(any()))
        .thenReturn(InmateDetail("A1234AA"))
    }

    @Test
    fun `can migrate multiple patients`() {
      val results = service.migrateInUnknownPatients(
        listOf(
          testRecord("header"),
          testRecord("valid"),
          testRecord("invalid_dob"),
          testRecord("invalid_gender"),
        ),
      )

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", "A1234AA", true, null),
        UnknownPatientResult("3/6170", null, false, "Date of birth 1965-02-33 invalid"),
        UnknownPatientResult("3/6170", null, false, "Gender of Y should be M or F"),
      )
      verify(prisonApiGateway).createPrisoner("O'Brien", "Steven", "John M", "M", LocalDate.of(1965, 2, 11), "cro", "pnc")
      verify(restrictedPatientService).dischargeToHospital(
        DischargeToHospitalRequest(
          "A1234AA",
          "Historical hospital release added to NOMIS for addition to Restricted Patients",
          "HOI",
          "BROADM",
          "HOI",
          LocalDate.of(2011, 9, 1).atStartOfDay()
        )
      )
    }

    @Test
    fun `will report on errors from create prisoner`() {
      whenever(prisonApiGateway.createPrisoner(anyString(), anyString(), anyString(), anyString(), any(), anyOrNull(), anyOrNull()))
        .thenThrow(webClientException(500, "some error"))

      val results = service.migrateInUnknownPatients(listOf(testRecord("header"), testRecord("valid")))

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", null, false, "Create prisoner failed due to: 500 some error")
      )
    }

    @Test
    fun `will report on errors from discharge to hospital`() {
      whenever(restrictedPatientService.dischargeToHospital(any())).thenThrow(webClientException(400, "some client error"))

      val results = service.migrateInUnknownPatients(listOf(testRecord("header"), testRecord("valid")))

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", "A1234AA", false, "Discharge to hospital failed due to: 400 some client error")
      )
    }

    @Test
    fun `will report on errors from update community NOMS number`() {
      whenever(communityApiGateway.updateNomsNumber(anyString(), anyString())).thenThrow(webClientException(404, "CRN not found"))

      val results = service.migrateInUnknownPatients(listOf(testRecord("header"), testRecord("valid")))

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", "A1234AA", false, "Update community NOMS number failed due to: 404 CRN not found")
      )
    }

    @Test
    fun `will report if community CRO number is different`() {
      whenever(communityApiGateway.updateNomsNumber(anyString(), anyString())).thenReturn(CommunityDetail("different_cro", "pnc"))

      val results = service.migrateInUnknownPatients(listOf(testRecord("header"), testRecord("valid")))

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", "A1234AA", false, "Community API returned CRO number 'different_cro' but we expected 'cro'")
      )
    }

    @Test
    fun `will report if community PNC number is different`() {
      whenever(communityApiGateway.updateNomsNumber(anyString(), anyString())).thenReturn(CommunityDetail("cro", "different_pnc"))

      val results = service.migrateInUnknownPatients(listOf(testRecord("header"), testRecord("valid")))

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", "A1234AA", false, "Community API returned PNC number 'different_pnc' but we expected 'pnc'")
      )
    }

    private fun webClientException(statusCode: Int, statusText: String) =
      WebClientResponseException.create(statusCode, statusText, HttpHeaders.EMPTY, ByteArray(0), Charset.defaultCharset())
  }

  @Nested
  inner class MigrateInUnknownPatientsDryRun {

    @Test
    fun `performs csv parsing and validation without doing anything else`() {
      val results = service.migrateInUnknownPatients(
        listOf(
          testRecord("header"),
          testRecord("valid"),
          testRecord("invalid_dob"),
        ),
        dryRun = true
      )

      assertThat(results).containsExactly(
        UnknownPatientResult("3/6170", null, true, null),
        UnknownPatientResult("3/6170", null, false, "Date of birth 1965-02-33 invalid"),
      )
      verify(prisonApiGateway, never()).createPrisoner(anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString())
      verify(restrictedPatientService, never()).dischargeToHospital(any())
    }
  }
}
