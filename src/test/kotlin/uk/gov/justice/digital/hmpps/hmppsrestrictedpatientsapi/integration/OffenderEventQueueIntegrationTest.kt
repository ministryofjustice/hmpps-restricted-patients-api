package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeOffenderMovementReceptionEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@ActiveProfiles(profiles = ["test"])
class OffenderEventQueueIntegrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Test
  @Disabled
  fun `will remove restricted patient from the system`() {
    dischargePrisonerWebClient(prisonerNumber = "A12346")
      .exchange()
      .expectStatus().isCreated

    getRestrictedPatient(prisonerNumber = "A12346")
      .exchange()
      .expectStatus().is2xxSuccessful

    hmppsQueueService.findByQueueId("offenderevents")!!.let {
      it.sqsClient.sendMessage(it.queueUrl, makeOffenderMovementReceptionEvent("A12346"))

      await untilCallTo { getNumberOfMessagesCurrentlyOnQueue(it) } matches { it == 0 }
    }

    getRestrictedPatient(prisonerNumber = "A12346")
      .exchange()
      .expectStatus().isNotFound
  }

  fun getNumberOfMessagesCurrentlyOnQueue(hmppsQueue: HmppsQueue): Int? {
    val queueAttributes =
      hmppsQueue.sqsClient.getQueueAttributes(hmppsQueue.queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
}
