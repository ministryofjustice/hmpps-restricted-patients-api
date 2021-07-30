package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.amazonaws.services.sqs.AmazonSQS
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeExternalMovementEventAsJson
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.QueueAdminService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
class OffenderEventQueueIntegrationTest : IntegrationTestBase() {
  @SpyBean
  @Qualifier("awsSqsClientForOffenderEvents")
  protected lateinit var awsSqsClient: AmazonSQS

  @Autowired
  lateinit var queueAdminService: QueueAdminService

  @Test
  fun `will remove restricted patient from the system`() {
    prisonApiMockServer.stubGetOffenderBooking(1L, "A12345")
    prisonApiMockServer.stubGetAgency("MDI", "INST", "Moorland")

    dischargePrisonerWebClient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().isCreated

    getRestrictedPatient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().is2xxSuccessful

    awsSqsClient.sendMessage(queueAdminService.queueUrl, makeExternalMovementEventAsJson("A12345"))

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    getRestrictedPatient(prisonerNumber = "A12345")
      .exchange()
      .expectStatus().isNotFound
  }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes =
      awsSqsClient.getQueueAttributes(queueAdminService.queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
}
