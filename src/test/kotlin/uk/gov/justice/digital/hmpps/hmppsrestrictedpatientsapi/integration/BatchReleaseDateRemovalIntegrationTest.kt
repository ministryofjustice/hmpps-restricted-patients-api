package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import java.time.LocalDateTime

@ActiveProfiles("test")
class BatchReleaseDateRemovalIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var restrictedPatientRepository: RestrictedPatientsRepository

  @Test
  fun `can call patients past their conditional release date`() {
    val restrictedPatient = RestrictedPatient(
      prisonerNumber = "A12345",
      fromLocationId = "MDI",
      hospitalLocationCode = "HAZLWD",
      supportingPrisonId = "LEI",
      dischargeTime = LocalDateTime.now(),
      commentText = "test"
    )
    restrictedPatientRepository.save(restrictedPatient)

    prisonerSearchApiMockServer.stubSearchByPrisonNumber("A12345")

    webTestClient.get()
      .uri("/process-past-date-restricted-patients")
      .exchange()
      .expectStatus().isOk
  }
}
