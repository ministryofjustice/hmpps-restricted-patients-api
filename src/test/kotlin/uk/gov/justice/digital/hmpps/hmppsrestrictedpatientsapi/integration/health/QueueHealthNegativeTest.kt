package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.health

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueHealth
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import java.net.URI

@ActiveProfiles("test")
class QueueHealthNegativeTest : IntegrationTestBase() {

  @TestConfiguration
  class TestConfig {
    @Bean
    fun badQueueHealth(hmppsSqsProperties: HmppsSqsProperties): HmppsQueueHealth {
      val sqsClient = SqsAsyncClient.builder()
        .region(Region.of(hmppsSqsProperties.region))
        .endpointOverride(URI.create(hmppsSqsProperties.localstackUrl))
        .credentialsProvider { AnonymousCredentialsProvider.create().resolveCredentials() }
        .build()
      return HmppsQueueHealth(HmppsQueue("missingQueueId", sqsClient, "missingQueue", sqsClient, "missingDlq"))
    }
  }

  @BeforeEach
  fun beforeEach() {
    prisonApiMockServer.resetMappings()
    prisonApiMockServer.stubHealth()
    prisonerSearchApiMockServer.stubHealth()
  }

  @Test
  fun `Health page reports down`() {
    webTestClient.get()
      .uri("/health")
      .headers(setHeaders(roles = emptyList()))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
      .expectBody()
      .jsonPath("$.status").isEqualTo("DOWN")
      .jsonPath("$.components.badQueueHealth.status").isEqualTo("DOWN")
      .jsonPath("$.components.badQueueHealth.details.dlqStatus").isEqualTo("DOWN")
  }
}
