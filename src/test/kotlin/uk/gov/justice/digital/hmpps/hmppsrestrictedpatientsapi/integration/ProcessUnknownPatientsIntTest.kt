package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientResult

@ActiveProfiles("test")
class ProcessUnknownPatientsIntTest : IntegrationTestBase() {

  @Nested
  inner class Security {
    @Test
    fun `should reject without a valid token`() {
      processUnknownPatientsWebClient()
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should reject without any roles`() {
      processUnknownPatientsWebClient(headers = setHeaders())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should reject request with wrong role`() {
      whenever(unknownPatientsService.migrateInUnknownPatient(anyString(), anyBoolean())).thenReturn(anyResult())

      processUnknownPatientsWebClient(headers = setHeaders(roles = listOf("ROLE_IS_WRONG")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should process request with valid token`() {
      whenever(unknownPatientsService.migrateInUnknownPatient(anyString(), anyBoolean())).thenReturn(anyResult())

      processUnknownPatientsWebClient(headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
    }

    private fun anyResult() = UnknownPatientResult(mhcsReference = "any", success = false)
  }

  @Nested
  inner class DryRun {
    @Test
    fun `should call service for a dry run`() {
      whenever(unknownPatientsService.migrateInUnknownPatientsDryRun(anyList())).thenReturn(listOf())

      webTestClient
        .post()
        .uri("/dryrun-unknown-patients")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .bodyValue(jacksonObjectMapper().writeValueAsString(listOf<String>()))
        .exchange()
        .expectStatus().isOk

      verify(unknownPatientsService).migrateInUnknownPatientsDryRun(any())
    }
  }

  @Nested
  inner class E2e {
    private val testFile = mapOf(
      "header" to """FILE_REFERENCE,FAMILY_NAME,FIRST_NAMES,Gender,DOB,Date of Sentence,Court sentenced at,Reason for reception,Prison received into,Under 21 at point of sentence?,Sentence type,Offence (list all current),CJA/Code,Sentence length,Offence to attach to sentence (most serious),AUTHORITY_FOR_DETENTION_DESCRIPTION,CURRENT_ESTABLISHMENT_DESCRIPTION,DATE_OF_HOSPITAL_ORDER,CRO Number,PNC Number,CRN""",
      "valid" to """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
      "invalid_dob" to """3/6170,O'Brien,Steven John M,M,1965-02-33,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01,cro,pnc,crn""",
    )
    private fun testRecord(test: String) = testFile[test] ?: throw IllegalStateException("Cannot find test data $test")

    @BeforeEach
    fun setUp() {
      prisonApiMockServer.stubCreatePrisoner("A1234AA")
      stubDischargePrisoner("A1234AA")
      communityApiMockServer.stubUpdateNomsNumber("crn")
      prisonerSearchApiMockServer.stubRefreshIndex("A1234AA")
      caseNotesApiMockServer.stubCreateCaseNote("A1234AA")
    }

    @Test
    fun `should process valid records`() {
      processUnknownPatientsWebClient(csvData = testRecord("valid"), headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.mhcsReference").isEqualTo("3/6170")
        .jsonPath("$.offenderNumber").isEqualTo("A1234AA")
        .jsonPath("$.success").isEqualTo("true")
        .jsonPath("$.errorMessage").isEmpty
    }

    @Test
    fun `should process invalid records`() {
      processUnknownPatientsWebClient(csvData = testRecord("invalid_dob"), headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.mhcsReference").isEqualTo("3/6170")
        .jsonPath("$.offenderNumber").isEqualTo(null)
        .jsonPath("$.success").isEqualTo("false")
        .jsonPath("$.errorMessage").isEqualTo("Date of birth 1965-02-33 invalid")
    }

    @Test
    fun `should handle create prisoner errors`() {
      prisonApiMockServer.stubCreatePrisonerError()

      processUnknownPatientsWebClient(csvData = testRecord("valid"), headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.mhcsReference").isEqualTo("3/6170")
        .jsonPath("$.offenderNumber").isEqualTo(null)
        .jsonPath("$.success").isEqualTo("false")
        .jsonPath("$.errorMessage").value<String> { assertThat(it).contains("Create prisoner failed due to:").contains("Some user message") }
    }

    @Test
    fun `should handle discharge to hospital server errors`() {
      prisonApiMockServer.stubDischargeToPrisonError("A1234AA")

      processUnknownPatientsWebClient(csvData = testRecord("valid"), headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.mhcsReference").isEqualTo("3/6170")
        .jsonPath("$.offenderNumber").isEqualTo("A1234AA")
        .jsonPath("$.success").isEqualTo("false")
        .jsonPath("$.errorMessage").value<String> { assertThat(it).contains("Discharge to hospital failed due to").contains("some error") }
    }

    @Test
    fun `should handle update NOMS number errors`() {
      communityApiMockServer.stubUpdateNomsNumberError("crn")

      processUnknownPatientsWebClient(csvData = testRecord("valid"), headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.mhcsReference").isEqualTo("3/6170")
        .jsonPath("$.offenderNumber").isEqualTo("A1234AA")
        .jsonPath("$.success").isEqualTo("false")
        .jsonPath("$.errorMessage").value<String> { assertThat(it).contains("Update community NOMS number failed due to:").contains("some error") }
    }

    @Test
    fun `should handle create case note errors`() {
      caseNotesApiMockServer.stubCreateCaseNotesError("A1234AA")

      processUnknownPatientsWebClient(csvData = testRecord("valid"), headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.mhcsReference").isEqualTo("3/6170")
        .jsonPath("$.offenderNumber").isEqualTo("A1234AA")
        .jsonPath("$.success").isEqualTo("false")
        .jsonPath("$.errorMessage").value<String> { assertThat(it).contains("Create case note failed due to:").contains("some error") }
    }
  }
}
