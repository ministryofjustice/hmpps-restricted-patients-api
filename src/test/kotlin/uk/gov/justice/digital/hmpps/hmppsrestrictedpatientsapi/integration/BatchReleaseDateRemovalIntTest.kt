package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import java.time.LocalDateTime

class BatchReleaseDateRemovalIntTest : IntegrationTestBase() {

  @Test
  fun `should remove prisoner when not in request scope`() {
    restrictedPatientRepository.save(
      RestrictedPatient(
        "A1234BC",
        "LEI",
        "HAZLWD",
        "LEI",
        LocalDateTime.now().minusDays(2),
        "Prisoner was released to hospital",
      ),
    )
    prisonerSearchApiMockServer.stubSearchByPrisonNumber(
      "A1234BC",
      """
      "prisonerNumber": "A1234BC",
      "bookingId": "1138058",
      "legalStatus": "SENTENCED",
      "indeterminateSentence": false,
      "recall": true,
      "sentenceExpiryDate": "2020-10-09"
      """.trimIndent(),
    )

    batchReleaseDateRemoval.removeNonLifePrisonersPastRelevantDate()

    verify(telemetryClient).trackEvent(
      "restricted-patient-batch-removal",
      mapOf("prisonerNumbers" to "A1234BC"),
      null,
    )
  }
}
