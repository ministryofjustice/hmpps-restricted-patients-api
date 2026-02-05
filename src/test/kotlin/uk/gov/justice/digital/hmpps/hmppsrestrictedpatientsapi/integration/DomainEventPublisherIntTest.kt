@file:Suppress("PropertyName")

package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

class DomainEventPublisherIntTest(@Autowired private val jsonMapper: JsonMapper) : IntegrationTestBase() {

  @Test
  fun `sends restricted patient added to the domain topic`() {
    domainEventPublisher.publishRestrictedPatientAdded(
      "some_offender",
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnDomainQueue() } matches { it!! > 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDomainQueue() } matches { it == 1 }

    val message = readNextDomainEventMessage()

    assertThatJson(message).node("eventType").isEqualTo("restricted-patients.patient.added")
    assertThatJson(message).node("version").isEqualTo(1)
    assertThatJson(message).node("occurredAt").isPresent()
    assertThatJson(message).node("publishedAt").isPresent()
    assertThatJson(message).node("description").isEqualTo("Prisoner is now a restricted patient")
    assertThatJson(message).node("personReference.identifiers").isEqualTo("[{\"type\":\"NOMS\",\"value\":\"some_offender\"}]")
    assertThatJson(message).node("additionalInformation.prisonerNumber").isEqualTo("some_offender")
  }

  @Test
  fun `sends restricted patient removed to the domain topic`() {
    domainEventPublisher.publishRestrictedPatientRemoved(
      "some_offender",
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnDomainQueue() } matches { it!! > 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDomainQueue() } matches { it == 1 }

    val message = readNextDomainEventMessage()

    assertThatJson(message).node("eventType").isEqualTo("restricted-patients.patient.removed")
    assertThatJson(message).node("version").isEqualTo(1)
    assertThatJson(message).node("occurredAt").isPresent()
    assertThatJson(message).node("publishedAt").isPresent()
    assertThatJson(message).node("description").isEqualTo("Prisoner no longer a restricted patient")
    assertThatJson(message).node("personReference.identifiers").isEqualTo("[{\"type\":\"NOMS\",\"value\":\"some_offender\"}]")
    assertThatJson(message).node("additionalInformation.prisonerNumber").isEqualTo("some_offender")
  }

  @Test
  fun `sends restricted patient supporting prison changed to the domain topic`() {
    domainEventPublisher.publishSupportingPrisonChanged(
      "some_offender",
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnDomainQueue() } matches { it!! > 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDomainQueue() } matches { it == 1 }

    val message = readNextDomainEventMessage()

    assertThatJson(message).node("eventType").isEqualTo("restricted-patients.patient.supporting-prison-changed")
    assertThatJson(message).node("version").isEqualTo(1)
    assertThatJson(message).node("occurredAt").isPresent()
    assertThatJson(message).node("publishedAt").isPresent()
    assertThatJson(message).node("description").isEqualTo("Supporting prisoner changed for restricted patient")
    assertThatJson(message).node("personReference.identifiers").isEqualTo("[{\"type\":\"NOMS\",\"value\":\"some_offender\"}]")
    assertThatJson(message).node("additionalInformation.prisonerNumber").isEqualTo("some_offender")
  }

  private fun getNumberOfMessagesCurrentlyOnDomainQueue(): Int? = testDomainEventQueue.sqsClient.countAllMessagesOnQueue(testDomainEventQueue.queueUrl).get()

  private fun readNextDomainEventMessage(): String {
    val updateResult = testDomainEventQueue.sqsClient.receiveFirstMessage()
    testDomainEventQueue.sqsClient.deleteLastMessage(updateResult)
    return jsonMapper.readValue<MsgBody>(updateResult.body()).Message
  }

  private fun SqsAsyncClient.receiveFirstMessage(): Message = receiveMessage(
    ReceiveMessageRequest.builder().queueUrl(testDomainEventQueue.queueUrl).build(),
  ).get().messages().first()

  private fun SqsAsyncClient.deleteLastMessage(result: Message) = deleteMessage(
    DeleteMessageRequest.builder().queueUrl(testDomainEventQueue.queueUrl).receiptHandle(result.receiptHandle()).build(),
  ).get()
}

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class MsgBody(val Message: String)
