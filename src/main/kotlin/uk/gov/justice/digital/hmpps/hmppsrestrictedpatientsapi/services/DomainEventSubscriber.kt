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
    val prisonerReceivedEvent = gson.fromJson(requestJson, PrisonerReceivedEvent::class.java)

    log.debug("Domain event received: {}", prisonerReceivedEvent.eventType)

    when (prisonerReceivedEvent.eventType) {
      "prison-offender-events.prisoner.receive" ->
        restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(
          prisonerReceivedEvent.additionalInformation.nomsNumber
        )
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
