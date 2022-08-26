package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.Clock
import java.time.LocalDate

@Service
class BatchReleaseDateRemoval(
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway,
  private val telemetryClient: TelemetryClient,
  private val clock: Clock,
) {

  fun removeNonLifePrisonersPastConditionalReleaseDate() {
    val now = LocalDate.now(clock)
    val toBeDeleted = restrictedPatientsRepository.findAll()
      .chunked(1000)
      .flatMap { chunk ->
        val results = prisonerSearchApiGateway.findByPrisonNumbers(chunk.map { it.prisonerNumber })
        results.filter { it.indeterminateSentence == false && it.conditionalReleaseDate?.isBefore(now) ?: false }
          .map { it.prisonerNumber }
      }

    if (toBeDeleted.isNotEmpty()) {
      telemetryClient.trackEvent(
        "restricted-patient-batch-removal",
        mapOf("prisonerNumbers" to toBeDeleted.joinToString()),
        null
      )
    }
  }
}
