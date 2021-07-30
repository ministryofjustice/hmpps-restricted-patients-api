package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("offender-events-sqs.provider")
class OffenderEventSubscriber(
  private val gson: Gson,
  private val restrictedPatientCleanup: RestrictedPatientCleanup
) {

  @JmsListener(destination = "\${offender-events-sqs.queue.name}", containerFactory = "jmsListenerContainerFactoryForOffenderEvents")
  fun handleEvents(requestJson: String?) {
    val (Message, MessageId, MessageAttributes) = gson.fromJson(requestJson, Message::class.java)
    val eventType = MessageAttributes.eventType.Value

    log.debug("Received message {} type {}", MessageId, eventType)

    when (eventType) {
      "EXTERNAL_MOVEMENT_RECORD-INSERTED" ->
        restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(
          gson.fromJson(
            Message,
            ExternalPrisonerMovementMessage::class.java
          )
        )
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class MessageAttributes(val eventType: EventType)
data class EventType(val Value: String, val Type: String)
data class Message(val Message: String, val MessageId: String, val MessageAttributes: MessageAttributes)

data class ExternalPrisonerMovementMessage(
  val bookingId: Long,
  val movementSeq: Long,
  val offenderIdDisplay: String,
  val fromAgencyLocationId: String,
  val toAgencyLocationId: String,
  val directionCode: String,
  val movementType: String
)
