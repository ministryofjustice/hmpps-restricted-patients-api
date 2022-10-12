package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.whenever
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
      whenever(unknownPatientsService.migrateInUnknownPatients(anyList())).thenReturn(listOf())

      processUnknownPatientsWebClient(headers = setHeaders(roles = listOf("ROLE_IS_WRONG")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should process request with valid token`() {
      whenever(unknownPatientsService.migrateInUnknownPatients(anyList())).thenReturn(listOf())

      processUnknownPatientsWebClient(headers = setHeaders(roles = listOf("ROLE_RESTRICTED_PATIENT_MIGRATION")))
        .exchange()
        .expectStatus().isOk
    }
  }
}
