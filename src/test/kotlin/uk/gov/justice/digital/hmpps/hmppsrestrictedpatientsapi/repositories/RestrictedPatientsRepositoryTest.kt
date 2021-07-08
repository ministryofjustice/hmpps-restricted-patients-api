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
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatients
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.security.UserSecurityUtils
import java.time.LocalDateTime

@ActiveProfiles("test")
@Import(AuditConfiguration::class, UserSecurityUtils::class)
@DataJpaTest
class RestrictedPatientsRepositoryTest {

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
      RestrictedPatients(
        prisonerNumber = "A12345",
        fromLocationId = "MDI",
        supportingPrisonId = "LEI",
        dischargeTime = now,
        hospitalLocationCode = "HAZLWD",
        commentText = "test"
      )
    ).id

    val entity = repository.findById(id!!).orElseThrow()

    assertThat(entity)
      .extracting(
        "id",
        "prisonerNumber",
        "fromLocationId",
        "supportingPrisonId",
        "dischargeTime",
        "hospitalLocationCode",
        "commentText",
        "createUserId"
      )
      .contains(id, "A12345", "MDI", "LEI", now, "HAZLWD", "test", "user")

    entityManager.remove(entity)
  }

  @Test
  fun `find by prison number`() {
    entityManager.persistAndFlush(
      RestrictedPatients(
        prisonerNumber = "A12345",
        fromLocationId = "MDI",
        supportingPrisonId = "LEI",
        dischargeTime = LocalDateTime.now(),
        hospitalLocationCode = "HAZLWD",
        commentText = "test"
      )
    )

    val entity = repository.findByPrisonerNumber("A12345")

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
