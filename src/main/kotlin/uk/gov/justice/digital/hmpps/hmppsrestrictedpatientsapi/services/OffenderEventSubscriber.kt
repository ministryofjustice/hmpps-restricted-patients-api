package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class OffenderMovementReception(
  val eventType: String,
  val offenderIdDisplay: String,
)

data class Event(val Message: String)

@Service
class OffenderEventSubscriber(
  private val gson: Gson,
  private val restrictedPatientCleanup: RestrictedPatientCleanup,
) {

  @SqsListener("offenderevents", factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "dps-core-restricted_patients_queue", kind = SpanKind.SERVER)
  fun handleEvents(requestJson: String?) {
    val event = gson.fromJson(requestJson, Event::class.java)
    val offenderMovementReception = gson.fromJson(event.Message, OffenderMovementReception::class.java)

    when (offenderMovementReception.eventType) {
      "OFFENDER_MOVEMENT-RECEPTION" ->
        restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(
          offenderMovementReception.offenderIdDisplay,
        )
      else -> log.warn("Unexpected offender event received: {}", offenderMovementReception.eventType)
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
