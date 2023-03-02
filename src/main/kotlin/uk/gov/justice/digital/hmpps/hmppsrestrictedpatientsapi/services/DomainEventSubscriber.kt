package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class HmppsDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
)

data class AdditionalInformation(
  val nomsNumber: String,
  val removedNomsNumber: String? = null,
)

@Service
class DomainEventSubscriber(
  private val gson: Gson,
  private val restrictedPatientCleanup: RestrictedPatientCleanup,
) {

  @SqsListener("domainevents", factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "Digital-Prison-Services-rp_queue_for_domain_events", kind = SpanKind.SERVER)
  fun handleEvents(requestJson: String?) {
    val event = gson.fromJson(requestJson, Event::class.java)
    with(gson.fromJson(event.Message, HmppsDomainEvent::class.java)) {
      when (eventType) {
        "prison-offender-events.prisoner.merged" ->
          restrictedPatientCleanup.mergeRestrictedPatient(
            additionalInformation.removedNomsNumber,
            additionalInformation.nomsNumber,
          )
        else -> log.warn("Unexpected domain event received: {}", eventType)
      }
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
