package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.GetQueueUrlResult
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerReceiveEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.QueueAdminService

internal class QueueAdminServiceTest {

  private val awsSqsClient = mock<AmazonSQS>()
  private val awsSqsDlqClient = mock<AmazonSQS>()
  private lateinit var queueAdminService: QueueAdminService

  @BeforeEach
  internal fun setUp() {
    whenever(awsSqsClient.getQueueUrl("event-queue")).thenReturn(GetQueueUrlResult().withQueueUrl("arn:eu-west-1:event-queue"))
    whenever(awsSqsDlqClient.getQueueUrl("event-dlq")).thenReturn(GetQueueUrlResult().withQueueUrl("arn:eu-west-1:event-dlq"))
    queueAdminService = QueueAdminService(
      awsSqsClient = awsSqsClient,
      awsSqsDlqClient = awsSqsDlqClient,
      queueName = "event-queue",
      dlqName = "event-dlq"
    )
  }

  @Nested
  inner class ClearAllDlqMessagesForEvent {
    @Test
    internal fun `will purge event dlq of messages`() {
      whenever(awsSqsDlqClient.getQueueUrl("event-dlq")).thenReturn(GetQueueUrlResult().withQueueUrl("arn:eu-west-1:event-dlq"))

      queueAdminService.clearAllDlqMessages()
      verify(awsSqsDlqClient).purgeQueue(
        check {
          assertThat(it.queueUrl).isEqualTo("arn:eu-west-1:event-dlq")
        }
      )
    }
  }

  @Nested
  inner class TransferAllEventDlqMessages {

    private val eventQueueUrl = "arn:eu-west-1:event-queue"
    private val eventDlqUrl = "arn:eu-west-1:event-dlq"

    @Test
    internal fun `will read single message from event dlq`() {
      stubDlqMessageCount(1)
      whenever(awsSqsDlqClient.receiveMessage(any<ReceiveMessageRequest>()))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234AA"))))

      queueAdminService.transferMessages()

      verify(awsSqsDlqClient).receiveMessage(
        check<ReceiveMessageRequest> {
          assertThat(it.queueUrl).isEqualTo(eventDlqUrl)
        }
      )
    }

    @Test
    internal fun `will read multiple messages from dlq`() {
      stubDlqMessageCount(3)
      whenever(awsSqsDlqClient.receiveMessage(any<ReceiveMessageRequest>()))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234AA"))))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234BB"))))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234CC"))))

      queueAdminService.transferMessages()

      verify(awsSqsDlqClient, times(3)).receiveMessage(
        check<ReceiveMessageRequest> {
          assertThat(it.queueUrl).isEqualTo(eventDlqUrl)
        }
      )
    }

    @Test
    internal fun `will send single message to the event queue`() {
      stubDlqMessageCount(1)
      whenever(awsSqsDlqClient.receiveMessage(any<ReceiveMessageRequest>()))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234AA"))))

      queueAdminService.transferMessages()

      verify(awsSqsClient).sendMessage(eventQueueUrl, makePrisonerReceiveEvent("Z1234AA"))
    }

    @Test
    internal fun `will send multiple messages to the event queue`() {
      stubDlqMessageCount(3)
      whenever(awsSqsDlqClient.receiveMessage(any<ReceiveMessageRequest>()))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234AA"))))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234BB"))))
        .thenReturn(ReceiveMessageResult().withMessages(Message().withBody(makePrisonerReceiveEvent("Z1234CC"))))

      queueAdminService.transferMessages()

      verify(awsSqsClient).sendMessage(eventQueueUrl, makePrisonerReceiveEvent("Z1234AA"))
      verify(awsSqsClient).sendMessage(eventQueueUrl, makePrisonerReceiveEvent("Z1234BB"))
      verify(awsSqsClient).sendMessage(eventQueueUrl, makePrisonerReceiveEvent("Z1234CC"))
    }

    private fun stubDlqMessageCount(count: Int) =
      whenever(awsSqsDlqClient.getQueueAttributes(eventDlqUrl, listOf("ApproximateNumberOfMessages")))
        .thenReturn(GetQueueAttributesResult().withAttributes(mutableMapOf("ApproximateNumberOfMessages" to count.toString())))
  }
}
