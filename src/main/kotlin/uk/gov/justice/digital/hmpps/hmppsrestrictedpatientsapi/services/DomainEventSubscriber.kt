package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service

data class PrisonerReceivedEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation
)

data class AdditionalInformation(
  val nomsNumber: String
)

data class Event(val Message: String)

@Service
@ConditionalOnProperty("domain-events-sqs.provider")
class DomainEventSubscriber(
  private val gson: Gson,
  private val restrictedPatientCleanup: RestrictedPatientCleanup
) {

  @JmsListener(
    destination = "\${domain-events-sqs.queue.name}",
    containerFactory = "jmsListenerContainerFactoryForDomainEvents"
  )
  fun handleEvents(requestJson: String?) {
    val event = gson.fromJson(requestJson, Event::class.java)
    val prisonerReceivedEvent = gson.fromJson(event.Message, PrisonerReceivedEvent::class.java)

    log.info("Domain event received: {}", prisonerReceivedEvent.eventType)

    when (prisonerReceivedEvent.eventType) {
      "prison-offender-events.prisoner.received" ->
        restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(
          prisonerReceivedEvent.additionalInformation.nomsNumber
        )
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
