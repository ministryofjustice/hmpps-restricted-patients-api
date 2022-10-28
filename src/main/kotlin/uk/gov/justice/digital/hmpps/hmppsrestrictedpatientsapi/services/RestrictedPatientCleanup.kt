package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import javax.transaction.Transactional

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
    restrictedPatientsRepository.findByIdOrNull(removedPrisonerNumber)?.let { removed ->
      // we don't expect this scenario to happen and there are lots of edge cases where we wouldn't be able to do the
      // merge automatically anyway so better to just throw our toys out of the pram.
      // Fix would normally be to manually remove the old prisoner number from the restricted patients database
      // and then work out where the prisoner is now and move them to the correct hospital if required.
      throw MergeRestrictedPatientNotImplemented(
        "Merge not implemented. Patient ${removed.prisonerNumber} was at hospital ${removed.hospitalLocationCode} but record merged into $prisonerNumber"
      )
    } ?: log.debug("Merge for removed prisoner {} ignored as not a restricted patient", removedPrisonerNumber)
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
