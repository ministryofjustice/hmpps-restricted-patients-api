package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import javax.transaction.Transactional

@Service
class RestrictedPatientCleanup(
  private val restrictedPatientsRepository: RestrictedPatientsRepository,
  private val telemetryClient: TelemetryClient
) {

  @Transactional
  fun deleteRestrictedPatientOnExternalMovementIntoPrison(prisonerNumber: String) {
    val restrictedPatient = restrictedPatientsRepository.findById(prisonerNumber).orElse(null) ?: return

    restrictedPatientsRepository.delete(restrictedPatient)

    telemetryClient.trackEvent(
      "restricted-patient-removed-on-prison-offender-events.prisoner.receive",
      mapOf(
        "prisonerNumber" to prisonerNumber,
      ),
      null
    )
  }
}
