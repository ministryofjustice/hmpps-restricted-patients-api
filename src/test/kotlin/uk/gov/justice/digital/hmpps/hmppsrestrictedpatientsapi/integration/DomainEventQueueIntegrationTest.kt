package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerMergeEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@ActiveProfiles(profiles = ["test"])
class DomainEventQueueIntegrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Test
  fun `will merge restricted patient`() {
    dischargePrisonerWebClient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().isCreated

    getRestrictedPatient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().is2xxSuccessful

    hmppsQueueService.findByQueueId("domainevents")!!.let {
      it.sqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(it.queueUrl).messageBody(makePrisonerMergeEvent("A12345", "A23456")).build(),
      ).get()

      await untilCallTo { getNumberOfMessagesCurrentlyOnQueue(it) } matches { it == 0 }
    }

    // TODO: Add in implementation of merge event
  }

  private fun getNumberOfMessagesCurrentlyOnQueue(hmppsQueue: HmppsQueue): Int? {
    val queueAttributes =
      hmppsQueue.sqsClient.getQueueAttributes(
        GetQueueAttributesRequest.builder().queueUrl(hmppsQueue.queueUrl).attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES).build(),
      ).get()
    return queueAttributes.attributes()[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES]?.toInt()
  }
}
