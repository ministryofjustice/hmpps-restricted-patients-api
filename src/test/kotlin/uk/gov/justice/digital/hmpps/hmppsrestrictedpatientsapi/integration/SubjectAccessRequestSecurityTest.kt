package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@WithMockAuthUser
@DisplayName("/subject-access-request security")
class SubjectAccessRequestSecurityTest : IntegrationTestBase() {

  @Test
  fun `access forbidden when no authority`() {
    webTestClient.get().uri("/subject-access-request?prn=A12345")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `access forbidden when no role`() {
    webTestClient.get().uri("/subject-access-request?prn=A12345")
      .headers(setHeaders(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `access forbidden with wrong role`() {
    webTestClient.get().uri("/subject-access-request?prn=A12345")
      .headers(setHeaders(roles = listOf("ROLE_BANANAS")))
      .exchange()
      .expectStatus().isForbidden
  }
}
