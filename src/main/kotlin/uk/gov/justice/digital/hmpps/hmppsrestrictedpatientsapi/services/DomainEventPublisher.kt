package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.amazonaws.services.sns.AmazonSNS
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate
import org.springframework.cloud.aws.messaging.core.TopicMessageChannel
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class DomainEvent(
  val eventType: String,
  val additionalInformation: RestrictedPatientRemovedAdditionalInformation,
  val version: Int,
  val occurredAt: LocalDateTime,
  val publishedAt: LocalDateTime,
  val description: String
)

data class RestrictedPatientRemovedAdditionalInformation(val prisonerNumber: String)

interface DomainEventPublisher {
  fun publishRestrictedPatientRemoved(prisonerNumber: String)
}

@Service
@ConditionalOnProperty(name = ["domain-events-sns.provider"], havingValue = "localstack")
class StubDomainEventPublisher : DomainEventPublisher {
  override fun publishRestrictedPatientRemoved(prisonerNumber: String) {}
}

@Service
@ConditionalOnProperty(name = ["domain-events-sns.provider"], havingValue = "aws")
class DomainEventPublisherImpl(
  @Qualifier("awsSnsClientForDomainEvents") client: AmazonSNS,
  @Value("\${domain-events-sns.topic.arn}") topicArn: String,
  private val gson: Gson
) : DomainEventPublisher {

  val topicTemplate = NotificationMessagingTemplate(client)
  val topicMessageChannel = TopicMessageChannel(client, topicArn)

  override fun publishRestrictedPatientRemoved(prisonerNumber: String) {
    val domainEvent = DomainEvent(
      eventType = "prison-offender-events.restricted-patient.removed",
      version = 1,
      occurredAt = LocalDateTime.now(),
      publishedAt = LocalDateTime.now(),
      description = "Prisoner no longer a restricted patient",
      additionalInformation = RestrictedPatientRemovedAdditionalInformation(prisonerNumber)
    )

    val payload = gson.toJson(domainEvent)

    topicTemplate.convertAndSend(
      topicMessageChannel,
      payload,
      mapOf("eventType" to domainEvent.eventType)
    )
  }
}
