package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
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

@Service
class DomainEventPublisher(
  private val topicTemplate: NotificationMessagingTemplate,
  private val topicMessageChannel: TopicMessageChannel,
  private val gson: Gson
) {

  fun publishRestrictedPatientRemoved(prisonerNumber: String) {
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
