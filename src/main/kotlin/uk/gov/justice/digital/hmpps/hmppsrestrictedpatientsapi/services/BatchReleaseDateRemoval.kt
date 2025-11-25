package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.Clock
import java.time.LocalDate

@Service
class BatchReleaseDateRemoval(
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val prisonerSearchApiService: PrisonerSearchApiService,
  private val restrictedPatientsService: RestrictedPatientsService,
  private val telemetryClient: TelemetryClient,
  private val domainEventPublisher: DomainEventPublisher,
  private val clock: Clock,
) {

  fun removeNonLifePrisonersPastRelevantDate() {
    log.info("Removing non-life prisoners with a release date in the past")
    val now = LocalDate.now(clock)
    val toBeDeleted = restrictedPatientsRepository.findAll()
      .chunked(1000)
      .flatMap { chunk ->
        val results = prisonerSearchApiService.findByPrisonNumbers(chunk.map { it.prisonerNumber })
        results.filter {
          it.indeterminateSentence == false &&
            (it.isNotRecallPastConditionalRelease(now) || it.isRecallPastSentenceExpiry(now))
        }
          .map { it.prisonerNumber }
      }

    if (toBeDeleted.isNotEmpty()) {
      telemetryClient.trackEvent(
        "restricted-patient-batch-removal",
        mapOf("prisonerNumbers" to toBeDeleted.joinToString()),
        null,
      )
      toBeDeleted.forEach {
        try {
          restrictedPatientsService.removeRestrictedPatient(it)
          domainEventPublisher.publishRestrictedPatientRemoved(it)
        } catch (ex: Exception) {
          log.error("restricted-patient-batch-removal failed for offender $it", ex)
        }
      }
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
