package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
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

@Service
class DomainEventPublisher(hmppsQueueService: HmppsQueueService, private val gson: Gson) {

  private val outboundTopic = hmppsQueueService.findByTopicId("domaineventsoutbound") ?: throw MissingTopicException("Could not find topic domaineventsoutbound")

  fun publishRestrictedPatientRemoved(prisonerNumber: String) {
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

    outboundTopic.snsClient.publish(
      PublishRequest(outboundTopic.arn, payload)
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(domainEvent.eventType))
        )
        .also { log.info("Published event to outbound topic. Type: ${domainEvent.eventType}") }
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
