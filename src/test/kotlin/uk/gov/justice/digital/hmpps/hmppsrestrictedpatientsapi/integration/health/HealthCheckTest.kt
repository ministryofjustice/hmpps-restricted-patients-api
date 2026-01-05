package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.health

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.IntegrationTestBase

class HealthCheckTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    prisonApiMockServer.resetMappings()
    prisonApiMockServer.stubHealth()
    prisonerSearchApiMockServer.stubHealth()
  }

  @Nested
  inner class DomainEventQueueTests {
    @Test
    fun `Queue health ok and dlq health ok, reports everything up`() {
      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.status").isEqualTo("UP")
        .jsonPath("$.components.domainevents-health.status").isEqualTo("UP")
        .jsonPath("$.components.domainevents-health.details.dlqStatus").isEqualTo("UP")
    }
  }

  @Nested
  inner class OffenderEventQueueTests {
    @Test
    fun `Queue health ok and dlq health ok, reports everything up`() {
      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.status").isEqualTo("UP")
        .jsonPath("$.components.offenderevents-health.status").isEqualTo("UP")
        .jsonPath("$.components.offenderevents-health.details.dlqStatus").isEqualTo("UP")
    }
  }

  @Test
  fun `Health page reports ok`() {
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Prison API health reports UP and OK`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.prisonApiHealthCheck.status").isEqualTo("UP")
      .jsonPath("components.prisonApiHealthCheck.details.HttpStatus").isEqualTo("200 OK")
  }

  @Test
  fun `Prisoner search API health reports UP and OK`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.prisonerSearchApiHealthCheck.status").isEqualTo("UP")
      .jsonPath("components.prisonerSearchApiHealthCheck.details.HttpStatus").isEqualTo("200 OK")
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }
}
