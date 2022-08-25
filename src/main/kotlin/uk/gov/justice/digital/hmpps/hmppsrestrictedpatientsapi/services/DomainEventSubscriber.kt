package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service

data class PrisonerReceivedEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation
)

data class AdditionalInformation(
  val nomsNumber: String
)

@Service
class DomainEventSubscriber(
  private val gson: Gson,
  private val restrictedPatientCleanup: RestrictedPatientCleanup
) {

  @JmsListener(
    destination = "domainevents",
    containerFactory = "hmppsQueueContainerFactoryProxy"
  )
  fun handleEvents(requestJson: String?) {
    val event = gson.fromJson(requestJson, Event::class.java)
    val prisonerReceivedEvent = gson.fromJson(event.Message, PrisonerReceivedEvent::class.java)

    when (prisonerReceivedEvent.eventType) {
      "prison-offender-events.prisoner.received" ->
        restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(
          prisonerReceivedEvent.additionalInformation.nomsNumber
        )
      else -> log.warn("Unexpected domain event received: {}", prisonerReceivedEvent.eventType)
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
