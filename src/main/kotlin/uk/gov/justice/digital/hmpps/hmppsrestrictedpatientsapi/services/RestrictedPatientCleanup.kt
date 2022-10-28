package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import javax.transaction.Transactional

@Service
class RestrictedPatientCleanup(
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val domainEventPublisher: DomainEventPublisher,
  private val telemetryClient: TelemetryClient,
  @Value("\${batch.merges.enabled:false}")
  private val mergesEnabled: Boolean,
) {

  @Transactional
  fun deleteRestrictedPatientOnExternalMovementIntoPrison(prisonerNumber: String) {
    restrictedPatientsRepository.findByIdOrNull(prisonerNumber)?.let {
      restrictedPatientsRepository.delete(it)

      domainEventPublisher.publishRestrictedPatientRemoved(prisonerNumber)

      telemetryClient.trackEvent(
        "restricted-patient-removed-cleanup",
        mapOf("prisonerNumber" to prisonerNumber),
        null
      )
    } ?: log.debug("Movement into prison for prisoner {} ignored as not a restricted patient", prisonerNumber)
  }

  fun mergeRestrictedPatient(removedPrisonerNumber: String?, prisonerNumber: String) {
    if (!mergesEnabled) {
      log.debug("Merge event disabled - old prisoner number {}, new prisoner number {}", removedPrisonerNumber, prisonerNumber)
      return
    }

    // TODO: Add in logic for merging restricted patient
    log.debug("Merge event received - old prisoner number {}, new prisoner number {}", removedPrisonerNumber, prisonerNumber)

    telemetryClient.trackEvent(
      "restricted-patient-merge-cleanup",
      mapOf("removedPrisonerNumber" to removedPrisonerNumber, "prisonerNumber" to prisonerNumber),
      null
    )
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
