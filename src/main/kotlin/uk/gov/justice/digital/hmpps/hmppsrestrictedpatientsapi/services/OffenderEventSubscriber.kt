package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service

data class OffenderMovementReception(
  val eventType: String,
  val offenderIdDisplay: String,
)

data class Event(val Message: String)

@Service
class OffenderEventSubscriber(
  private val gson: Gson,
  private val restrictedPatientCleanup: RestrictedPatientCleanup
) {

  @JmsListener(
    destination = "offenderevents",
    containerFactory = "hmppsQueueContainerFactoryProxy"
  )
  fun handleEvents(requestJson: String?) {
    val event = gson.fromJson(requestJson, Event::class.java)
    val offenderMovementReception = gson.fromJson(event.Message, OffenderMovementReception::class.java)

    log.info("Offender event received: {}", offenderMovementReception.eventType)

    // when (offenderMovementReception.eventType) {
    //   "OFFENDER_MOVEMENT-RECEPTION" ->
    //     restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(
    //       offenderMovementReception.offenderIdDisplay
    //     )
    // }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
