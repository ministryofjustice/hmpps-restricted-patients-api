package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.BatchReleaseDateRemoval
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientService

@RestController
class RestrictedPatientsBatchController(
  private val batchReleaseDateRemoval: BatchReleaseDateRemoval,
  private val unknownPatientService: UnknownPatientService
) {
  @Hidden
  @PostMapping(value = ["/process-past-date-restricted-patients"])
  /**
   * Internal endpoint to find all restricted patients that have determinate sentences (non lifers) where the
   * conditional release date is in the past so shouldn't be handled by restricted patients anymore.
   */
  fun processPastDateRestrictedPatients(): Unit =
    batchReleaseDateRemoval.removeNonLifePrisonersPastRelevantDate()

  @Hidden
  @PostMapping(value = ["/process-unknown-patient"])
  @PreAuthorize("hasRole('RESTRICTED_PATIENT_MIGRATION')")
  /**
   * Internal endpoint to migrate unknown patients into Nomis and add them to Restricted Patients
   */
  fun processUnknownPatient(@RequestBody patient: String): UnknownPatientResult =
    unknownPatientService.migrateInUnknownPatient(patient)

  @Hidden
  @PostMapping(value = ["/dryrun-unknown-patient"])
  @PreAuthorize("hasRole('RESTRICTED_PATIENT_MIGRATION')")
  /**
   * DRY RUN version - this will validate input only but perform no actions
   */
  fun dryRunProcessUnknownPatients(@RequestBody patient: String): UnknownPatientResult =
    unknownPatientService.migrateInUnknownPatient(patient, true)
}
