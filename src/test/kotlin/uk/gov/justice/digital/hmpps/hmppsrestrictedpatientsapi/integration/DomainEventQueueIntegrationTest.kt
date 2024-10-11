package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.data.repository.findByIdOrNull
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeOutboundPrisonerSearchReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerMergeEvent
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class DomainEventQueueIntegrationTest : IntegrationTestBase() {

  @Test
  fun `will merge restricted patient`() {
    dischargePrisonerWebClient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().isCreated

    getRestrictedPatient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().is2xxSuccessful

    domainEventQueue.run {
      sqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(makePrisonerMergeEvent("A12345", "A23456")).build(),
      ).get()

      await untilCallTo { sqsClient.countMessagesOnQueue(queueUrl).get() } matches { it == 0 }
    }

    // TODO: Add in implementation of merge event
  }

  @Nested
  @DisplayName("prisoner-offender-search.prisoner.released")
  inner class PrisonerReleased {
    @BeforeEach
    fun `set up`() {
      restrictedPatientRepository.deleteById("A1234AA")
    }

    @Test
    fun `should add a prisoner to Restricted Patients`() {
      prisonApiMockServer.stubGetLatestMovementsReleased("A1234AA", "HAZLWD")
      prisonApiMockServer.stubGetAgency("HAZLWD", "HOSPITAL")

      domainEventQueue.run {
        sqsClient.sendMessage(
          SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(makeOutboundPrisonerSearchReleasedEvent("A1234AA"))
            .build(),
        ).get()
      }

      await untilAsserted {
        verify(domainEventPublisher).publishRestrictedPatientAdded("A1234AA")
        assertThat(restrictedPatientRepository.findByIdOrNull("A1234AA")).isNotNull
      }
    }

    @Test
    fun `should not add a prisoner to Restricted Patients`(output: CapturedOutput) {
      prisonApiMockServer.stubGetLatestMovementsReleased("A1234AA", "MDI")
      prisonApiMockServer.stubGetAgency("MDI", "INST")

      domainEventQueue.run {
        sqsClient.sendMessage(
          SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(makeOutboundPrisonerSearchReleasedEvent("A1234AA"))
            .build(),
        ).get()
      }

      await untilAsserted {
        assertThat(output.out).contains("Ignoring release of prisoner")
      }
      verify(domainEventPublisher, never()).publishRestrictedPatientAdded("A1234AA")
      assertThat(restrictedPatientRepository.findByIdOrNull("A1234AA")).isNull()
      with(domainEventQueue) {
        assertThat(sqsDlqClient?.countMessagesOnQueue(dlqUrl!!)?.get()).isEqualTo(0)
      }
    }

    @Test
    fun `should put event onto the DLQ if there is an error`() {
      prisonApiMockServer.stubServerError(WireMock::get)

      domainEventQueue.run {
        sqsClient.sendMessage(
          SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(makeOutboundPrisonerSearchReleasedEvent("A1234AA"))
            .build(),
        ).get()
      }

      await untilAsserted {
        with(domainEventQueue) {
          assertThat(sqsDlqClient?.countMessagesOnQueue(dlqUrl!!)?.get()).isEqualTo(1)
        }
      }
      verify(domainEventPublisher, never()).publishRestrictedPatientAdded("A1234AA")
      assertThat(restrictedPatientRepository.findByIdOrNull("A1234AA")).isNull()
    }
  }
}
