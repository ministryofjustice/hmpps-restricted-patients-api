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
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictivePatient
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
      RestrictivePatient(
        fromLocationId = "MDI",
        supportingPrisonId = "LEI",
        dischargeTime = now,
        hospitalLocationCode = "HAZLWD",
        commentText = "test"
      )
    ).id

    assertThat(repository.findById(id).orElseThrow())
      .extracting(
        "id",
        "fromLocationId",
        "supportingPrisonId",
        "dischargeTime",
        "hospitalLocationCode",
        "commentText",
        "createUserId"
      )
      .contains(id, "MDI", "LEI", now, "HAZLWD", "test", "user")
  }
}
