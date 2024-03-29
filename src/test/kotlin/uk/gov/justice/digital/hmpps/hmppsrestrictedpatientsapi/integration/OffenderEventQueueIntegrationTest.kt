package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeOffenderMovementReceptionEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue

class OffenderEventQueueIntegrationTest : IntegrationTestBase() {
  @Test
  fun `will remove restricted patient from the system`() {
    dischargePrisonerWebClient(prisonerNumber = "A12346")
      .exchange()
      .expectStatus().isCreated

    getRestrictedPatient(prisonerNumber = "A12346")
      .exchange()
      .expectStatus().is2xxSuccessful

    offenderEventQueue.let {
      it.sqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(it.queueUrl).messageBody(makeOffenderMovementReceptionEvent("A12346")).build(),
      ).get()

      await untilCallTo { getNumberOfMessagesCurrentlyOnQueue(it) } matches { it == 0 }
    }

    getRestrictedPatient(prisonerNumber = "A12346")
      .exchange()
      .expectStatus().isNotFound
  }

  private fun getNumberOfMessagesCurrentlyOnQueue(hmppsQueue: HmppsQueue): Int? {
    val queueAttributes =
      hmppsQueue.sqsClient.getQueueAttributes(
        GetQueueAttributesRequest.builder().queueUrl(hmppsQueue.queueUrl).attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES).build(),
      ).get()
    return queueAttributes.attributes()[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES]?.toInt()
  }
}
