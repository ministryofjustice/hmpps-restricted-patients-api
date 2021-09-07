package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.amazonaws.services.sns.AmazonSNS
import com.google.gson.Gson
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate
import org.springframework.cloud.aws.messaging.core.TopicMessageChannel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DomainEvent(
  val eventType: String,
  val additionalInformation: RestrictedPatientRemovedAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val publishedAt: String,
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

  private val topicTemplate = NotificationMessagingTemplate(client)
  private val topicMessageChannel = TopicMessageChannel(client, topicArn)

  override fun publishRestrictedPatientRemoved(prisonerNumber: String) {
    val now = LocalDateTime.now().atZone(ZoneId.of("Europe/London")).toOffsetDateTime()
      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    val domainEvent = DomainEvent(
      eventType = "restricted-patients.patient.removed",
      version = 1,
      occurredAt = now,
      publishedAt = now,
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
