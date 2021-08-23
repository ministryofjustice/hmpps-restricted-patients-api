package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.AuditConfiguration
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.security.UserSecurityUtils
import java.time.LocalDateTime

@ActiveProfiles("test")
@Import(AuditConfiguration::class, UserSecurityUtils::class)
@DataJpaTest
class RestrictedPatientRepositoryTest {

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var repository: RestrictedPatientsRepository

  @BeforeEach
  fun beforeEach() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")
  }

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
        commentText = "test"
      )
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
        "createUserId"
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
        commentText = "test"
      )
    )

    val entity = repository.findById("A12345").get()

    assertThat(entity)
      .extracting(
        "prisonerNumber",
        "fromLocationId",
        "supportingPrisonId",
        "hospitalLocationCode",
        "commentText",
        "createUserId"
      )
      .contains("A12345", "MDI", "LEI", "HAZLWD", "test", "user")

    entityManager.remove(entity)
  }
}
