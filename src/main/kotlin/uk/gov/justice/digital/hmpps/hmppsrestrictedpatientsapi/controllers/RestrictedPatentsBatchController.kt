package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.BatchReleaseDateRemoval

@RestController
class RestrictedPatentsBatchController(private val batchReleaseDateRemoval: BatchReleaseDateRemoval) {
  @Hidden
  @GetMapping(value = ["/process-past-date-restricted-patients"])
  /**
   * Internal endpoint to find all restricted patients that have determinate sentences (non lifers) where the
   * conditional release date is in the past so shouldn't be handled by restricted patients anymore.
   */
  fun processPastDateRestrictedPatients(): Unit =
    batchReleaseDateRemoval.removeNonLifePrisonersPastConditionalReleaseDate()
}
