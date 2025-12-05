package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.AuditConfiguration
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDateTime

@ActiveProfiles("test")
@Import(AuditConfiguration::class, HmppsAuthenticationHolder::class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@WithMockAuthUser("user")
class RestrictedPatientRepositoryTest {

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var repository: RestrictedPatientsRepository

  @Test
  fun `retrieve restricted patient`() {
    val now = LocalDateTime.now()

    val id = entityManager.persistAndFlush(
      RestrictedPatient(
        prisonerNumber = "A12345",
        fromLocationId = "MDI",
        supportingPrisonId = "LEI",
        dischargeTime = now,
        hospitalLocationCode = "HAZLWD",
        commentText = "test",
      ),
    ).prisonerNumber

    val entity = repository.findById(id).orElseThrow()

    assertThat(entity)
      .extracting(
        "prisonerNumber",
        "fromLocationId",
        "supportingPrisonId",
        "dischargeTime",
        "hospitalLocationCode",
        "commentText",
        "createUserId",
      )
      .contains("A12345", "MDI", "LEI", now, "HAZLWD", "test", "user")

    entityManager.remove(entity)
  }

  @Test
  fun `find by prison number`() {
    entityManager.persistAndFlush(
      RestrictedPatient(
        prisonerNumber = "A12345",
        fromLocationId = "MDI",
        supportingPrisonId = "LEI",
        dischargeTime = LocalDateTime.now(),
        hospitalLocationCode = "HAZLWD",
        commentText = "test",
      ),
    )

    val entity = repository.findById("A12345").get()

    assertThat(entity)
      .extracting(
        "prisonerNumber",
        "fromLocationId",
        "supportingPrisonId",
        "hospitalLocationCode",
        "commentText",
        "createUserId",
      )
      .contains("A12345", "MDI", "LEI", "HAZLWD", "test", "user")

    entityManager.remove(entity)
  }
}
