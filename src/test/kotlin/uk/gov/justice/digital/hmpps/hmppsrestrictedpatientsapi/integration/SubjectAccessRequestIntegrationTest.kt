package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDateTime
import javax.sql.DataSource

@WithMockAuthUser
@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest @Autowired constructor(
  private val dataSource: DataSource,
  private val entityManager: EntityManager,
  private val sarIntegrationTestHelper: SarIntegrationTestHelper,
) : IntegrationTestBase(),
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarApiDataTest,
  SarReportTest {

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String = PRN

  override fun setupTestData() {
    restrictedPatientRepository.save(
      RestrictedPatient(
        prisonerNumber = PRN,
        fromLocationId = "MDI",
        hospitalLocationCode = "HAZLWD",
        supportingPrisonId = "LEI",
        dischargeTime = LocalDateTime.parse("2020-10-09T00:00:00"),
        commentText = "Released to hospital (Restricted Patients migration)",
      ),
    )
  }

  private companion object {
    const val PRN = "G1072GT"
  }
}
