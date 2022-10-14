package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

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
      whenever(unknownPatientsService.migrateInUnknownPatients(anyList(), anyBoolean())).thenReturn(listOf())

      processUnknownPatientsWebClient(headers = setHeaders(roles = listOf("ROLE_IS_WRONG")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should process request with valid token`() {
      whenever(unknownPatientsService.migrateInUnknownPatients(anyList(), anyBoolean())).thenReturn(listOf())

      processUnknownPatientsWebClient(headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class DryRun {
    @Test
    fun `should call service for a dry run`() {
      whenever(unknownPatientsService.migrateInUnknownPatients(anyList(), anyBoolean())).thenReturn(listOf())

      webTestClient
        .post()
        .uri("/dryrun-unknown-patients")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .bodyValue(jacksonObjectMapper().writeValueAsString(listOf<String>()))
        .exchange()
        .expectStatus().isOk

      verify(unknownPatientsService).migrateInUnknownPatients(any(), eq(true))
    }
  }

  @Nested
  inner class E2e {
    private val testFile = mapOf(
      "header" to """FILE_REFERENCE,FAMILY_NAME,FIRST_NAMES,Gender,DOB,Date of Sentence,Court sentenced at,Reason for reception,Prison received into,Under 21 at point of sentence?,Sentence type,Offence (list all current),CJA/Code,Sentence length,Offence to attach to sentence (most serious),AUTHORITY_FOR_DETENTION_DESCRIPTION,CURRENT_ESTABLISHMENT_DESCRIPTION,DATE_OF_HOSPITAL_ORDER""",
      "valid" to """3/6170,O'Brien,Steven John M,M,1965-02-11,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01""",
      "invalid_dob" to """3/6170,O'Brien,Steven John M,M,1965-02-33,12/9/2009,Sheffield Crown Court,Imprisonment,HMP High Down,no,Discretionary life,Attempted murder,,Tariff - 15 years,Attempted murder ,S45A - MHA 1983 - Hospital & Limitation Direction,Broadmoor Hospital,2011-09-01""",
    )
    private fun testRecord(test: String) = testFile[test] ?: throw IllegalStateException("Cannot find test data $test")

    @Test
    fun `should process valid records`() {
      prisonApiMockServer.stubCreatePrisoner("A1234AA")
      stubDischargePrisoner("A1234AA")
      val testData = listOf(testRecord("header"), testRecord("valid"))

      processUnknownPatientsWebClient(csvData = testData, headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].mhcsReference").isEqualTo("3/6170")
        .jsonPath("$[0].offenderNumber").isEqualTo("A1234AA")
        .jsonPath("$[0].success").isEqualTo("true")
        .jsonPath("$[0].errorMessage").isEmpty
    }

    @Test
    fun `should process invalid records`() {
      val testData = listOf(testRecord("header"), testRecord("invalid_dob"))

      processUnknownPatientsWebClient(csvData = testData, headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].mhcsReference").isEqualTo("3/6170")
        .jsonPath("$[0].offenderNumber").isEqualTo(null)
        .jsonPath("$[0].success").isEqualTo("false")
        .jsonPath("$[0].errorMessage").isEqualTo("Date of birth 1965-02-33 invalid")
    }

    @Test
    fun `should handle create prisoner server errors`() {
      prisonApiMockServer.stubServerError(WireMock::post)
      val testData = listOf(testRecord("header"), testRecord("valid"))

      processUnknownPatientsWebClient(csvData = testData, headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].mhcsReference").isEqualTo("3/6170")
        .jsonPath("$[0].offenderNumber").isEqualTo(null)
        .jsonPath("$[0].success").isEqualTo("false")
        .jsonPath("$[0].errorMessage").value<String> { assertThat(it).contains("Create prisoner failed due to: 503") }
    }

    @Test
    fun `should handle discharge to hospital server errors`() {
      prisonApiMockServer.stubCreatePrisoner("A1234AA")
      stubDischargePrisoner("A1234AA")
      prisonApiMockServer.stubDischargeToPrisonError("A1234AA")
      val testData = listOf(testRecord("header"), testRecord("valid"))

      processUnknownPatientsWebClient(csvData = testData, headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].mhcsReference").isEqualTo("3/6170")
        .jsonPath("$[0].offenderNumber").isEqualTo("A1234AA")
        .jsonPath("$[0].success").isEqualTo("false")
        .jsonPath("$[0].errorMessage").value<String> { assertThat(it).contains("Discharge to hospital failed due to: 400") }
    }

    @Test
    fun `should process multiple records`() {
      prisonApiMockServer.stubCreatePrisoner("A1234AA")
      stubDischargePrisoner("A1234AA")
      val testData = listOf(testRecord("header"), testRecord("valid"), testRecord("invalid_dob"))

      processUnknownPatientsWebClient(csvData = testData, headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].mhcsReference").isEqualTo("3/6170")
        .jsonPath("$[0].offenderNumber").isEqualTo("A1234AA")
        .jsonPath("$[0].success").isEqualTo("true")
        .jsonPath("$[0].errorMessage").isEmpty
        .jsonPath("$[1].mhcsReference").isEqualTo("3/6170")
        .jsonPath("$[1].offenderNumber").isEqualTo(null)
        .jsonPath("$[1].success").isEqualTo("false")
        .jsonPath("$[1].errorMessage").isEqualTo("Date of birth 1965-02-33 invalid")
    }
  }
}