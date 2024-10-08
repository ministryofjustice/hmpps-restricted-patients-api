package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.PersonReference.Companion.withNomsNumber
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DomainEvent(
  val eventType: String,
  val additionalInformation: RestrictedPatientAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val publishedAt: String,
  val description: String,
  val personReference: PersonReference = withNomsNumber(additionalInformation.prisonerNumber),
)

data class PersonReference(val identifiers: List<Identifier>) {
  companion object {
    fun withNomsNumber(prisonNumber: String) = PersonReference(listOf(Identifier("NOMS", prisonNumber)))
  }

  data class Identifier(val type: String, val value: String)
}

data class RestrictedPatientAdditionalInformation(val prisonerNumber: String)

@Service
class DomainEventPublisher(hmppsQueueService: HmppsQueueService, private val gson: Gson) {

  private val outboundTopic = hmppsQueueService.findByTopicId("domaineventsoutbound")
    ?: throw MissingTopicException("Could not find topic domaineventsoutbound")

  fun publishRestrictedPatientAdded(prisonerNumber: String) {
    publishRestrictedPatientEvent(
      eventType = "restricted-patients.patient.added",
      description = "Prisoner is now a restricted patient",
      prisonerNumber = prisonerNumber,
    )
  }
  fun publishRestrictedPatientRemoved(prisonerNumber: String) {
    publishRestrictedPatientEvent(
      eventType = "restricted-patients.patient.removed",
      description = "Prisoner no longer a restricted patient",
      prisonerNumber = prisonerNumber,
    )
  }

  private fun publishRestrictedPatientEvent(eventType: String, description: String, prisonerNumber: String) {
    val now = LocalDateTime.now().atZone(ZoneId.of("Europe/London")).toOffsetDateTime()
      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    val domainEvent = DomainEvent(
      eventType = eventType,
      version = 1,
      occurredAt = now,
      publishedAt = now,
      description = description,
      additionalInformation = RestrictedPatientAdditionalInformation(prisonerNumber),
    )

    val payload = gson.toJson(domainEvent)

    outboundTopic.publish(
      eventType = domainEvent.eventType,
      event = payload,
    ).also { log.info("Published event to outbound topic. Type: ${domainEvent.eventType}") }
  }

  fun publishSupportingPrisonChanged(prisonerNumber: String) {
    publishRestrictedPatientEvent(
      eventType = "restricted-patients.patient.supporting-prison-changed",
      description = "Supporting prisoner changed for restricted patient",
      prisonerNumber = prisonerNumber,
    )
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
