package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.QueueAttributeName
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.DlqStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.QueueAttributes
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.QueueHealth
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : IntegrationTestBase() {

  @SpyBean
  @Qualifier("awsSqsClientForDomainEvents")
  protected lateinit var awsSqsClient: AmazonSQS

  @Autowired
  private lateinit var queueHealth: QueueHealth

  @Autowired
  @Value("\${domain-events-sqs.queue.name}")
  private lateinit var domainEventsQueueName: String

  @Autowired
  @Value("\${domain-events-sqs.dlq.name}")
  private lateinit var domainEventsDlqName: String

  @BeforeEach
  fun beforeEach() {
    prisonApiMockServer.resetMappings()
    prisonApiMockServer.stubHealth()
    prisonerSearchApiMockServer.stubHealth()
  }

  @AfterEach
  fun tearDown() {
    ReflectionTestUtils.setField(queueHealth, "queueName", domainEventsQueueName)
    ReflectionTestUtils.setField(queueHealth, "dlqName", domainEventsDlqName)
  }

  @Nested
  inner class DomainEventQueueTests {
    @Test
    fun `Queue does not exist reports down`() {
      ReflectionTestUtils.setField(queueHealth, "queueName", "missing_queue")

      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()
        .jsonPath("$.status").isEqualTo("DOWN")
        .jsonPath("$.components.queueHealth.status").isEqualTo("DOWN")
    }

    @Test
    fun `Queue health ok and dlq health ok, reports everything up`() {
      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.status").isEqualTo("UP")
        .jsonPath("$.components.queueHealth.status").isEqualTo("UP")
        .jsonPath("$.components.queueHealth.details.dlqStatus").isEqualTo(DlqStatus.UP.description)
    }

    @Test
    fun `Dlq health reports interesting attributes`() {
      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectBody()
        .jsonPath("$.components.queueHealth.details.${QueueAttributes.MESSAGES_ON_DLQ.healthName}").isEqualTo(0)
    }

    @Test
    fun `Dlq down brings main health and queue health down`() {
      mockQueueWithoutRedrivePolicyAttributes()

      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()
        .jsonPath("$.status").isEqualTo("DOWN")
        .jsonPath("$.components.queueHealth.status").isEqualTo("DOWN")
        .jsonPath("$.components.queueHealth.details.dlqStatus").isEqualTo(DlqStatus.NOT_ATTACHED.description)
    }

    @Test
    fun `Main queue has no redrive policy reports dlq down`() {
      mockQueueWithoutRedrivePolicyAttributes()

      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()
        .jsonPath("$.components.queueHealth.details.dlqStatus").isEqualTo(DlqStatus.NOT_ATTACHED.description)
    }

    @Test
    fun `Dlq not found reports dlq down`() {
      ReflectionTestUtils.setField(queueHealth, "dlqName", "missing_queue")

      webTestClient.get()
        .uri("/health")
        .headers(setHeaders())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()
        .jsonPath("$.components.queueHealth.details.dlqStatus").isEqualTo(DlqStatus.NOT_FOUND.description)
    }

    private fun mockQueueWithoutRedrivePolicyAttributes() {
      val queueName = ReflectionTestUtils.getField(queueHealth, "queueName") as String
      val queueUrl = awsSqsClient.getQueueUrl(queueName)
      whenever(
        awsSqsClient.getQueueAttributes(
          GetQueueAttributesRequest(queueUrl.queueUrl).withAttributeNames(
            listOf(
              QueueAttributeName.All.toString()
            )
          )
        )
      )
        .thenReturn(GetQueueAttributesResult())
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
  fun `Health info reports version`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        }
      )
  }

  @Test
  fun `Prison API health reports UP and OK`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.prisonApiHealthCheck.status").value(
        Consumer<String> {
          assertThat(it).isEqualTo("UP")
        }
      )
      .jsonPath("components.prisonApiHealthCheck.details.HttpStatus").value(
        Consumer<String> {
          assertThat(it).isEqualTo("OK")
        }
      )
  }

  @Test
  fun `Prisoner search API health reports UP and OK`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.prisonerSearchApiHealthCheck.status").value(
        Consumer<String> {
          assertThat(it).isEqualTo("UP")
        }
      )
      .jsonPath("components.prisonerSearchApiHealthCheck.details.HttpStatus").value(
        Consumer<String> {
          assertThat(it).isEqualTo("OK")
        }
      )
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
