package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDateTime

@WithMockAuthUser
class BatchReleaseDateRemovalIntegrationTest : IntegrationTestBase() {

  @Test
  fun `can call patients past their conditional release date`() {
    val restrictedPatient = RestrictedPatient(
      prisonerNumber = "A12345",
      fromLocationId = "MDI",
      hospitalLocationCode = "HAZLWD",
      supportingPrisonId = "LEI",
      dischargeTime = LocalDateTime.now(),
      commentText = "test",
    )
    restrictedPatientRepository.save(restrictedPatient)

    prisonerSearchApiMockServer.stubSearchByPrisonNumber("A12345")

    webTestClient.post()
      .uri("/process-past-date-restricted-patients")
      .exchange()
      .expectStatus().isOk
  }
}
