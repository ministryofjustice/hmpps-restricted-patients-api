package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository

@Service
class RestrictedPatientCleanup(
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val domainEventPublisher: DomainEventPublisher,
  private val telemetryClient: TelemetryClient,
) {

  @Transactional
  fun deleteRestrictedPatientOnExternalMovementIntoPrison(prisonerNumber: String) {
    restrictedPatientsRepository.findByIdOrNull(prisonerNumber)?.let {
      restrictedPatientsRepository.delete(it)

      domainEventPublisher.publishRestrictedPatientRemoved(prisonerNumber)

      telemetryClient.trackEvent(
        "restricted-patient-removed-cleanup",
        mapOf("prisonerNumber" to prisonerNumber),
        null,
      )
    } ?: log.debug("Movement into prison for prisoner {} ignored as not a restricted patient", prisonerNumber)
  }

  fun mergeRestrictedPatient(removedPrisonerNumber: String?, prisonerNumber: String) {
    // If we find that either side of the merge was a restricted patient then we throw our toys out of the pram.
    // Fix would normally be to manually remove the old prisoner number from the restricted patients database
    // and then work out where the prisoner is now and move them to the correct hospital if required.  See README.md.
    restrictedPatientsRepository.findByIdOrNull(removedPrisonerNumber)?.let {
      throw MergeRestrictedPatientNotImplemented(
        "Merge not implemented. Patient ${it.prisonerNumber} was at hospital ${it.hospitalLocationCode} but record merged into $prisonerNumber",
      )
    }

    restrictedPatientsRepository.findByIdOrNull(prisonerNumber)?.let {
      throw MergeRestrictedPatientNotImplemented(
        "Merge not implemented. Patient ${it.prisonerNumber} is at hospital ${it.hospitalLocationCode}.  Record merged from $removedPrisonerNumber",
      )
    }

    log.debug("Merge from prisoner {} to prisoner {} ignored as neither a restricted patient", removedPrisonerNumber, prisonerNumber)
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  class MergeRestrictedPatientNotImplemented(message: String) : Throwable(message) {
    init {
      log.error(message)
    }
  }
}
