package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.amazonaws.services.sns.AmazonSNS
import com.google.gson.Gson
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate
import org.springframework.cloud.aws.messaging.core.TopicMessageChannel
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

class StubDomainEventPublisher : DomainEventPublisher {
  override fun publishRestrictedPatientRemoved(prisonerNumber: String) {}
}

class DomainEventPublisherImpl(client: AmazonSNS, topicArn: String, private val gson: Gson) :
  DomainEventPublisher {

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
