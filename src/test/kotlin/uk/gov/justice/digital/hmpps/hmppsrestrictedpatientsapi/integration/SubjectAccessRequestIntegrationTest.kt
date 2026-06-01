package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import javax.sql.DataSource

@WithMockAuthUser
@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTestBase(),
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarApiDataTest,
  SarReportTest {

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun setupTestData() {}

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String = "G1072GT"

  @Test
  @Sql("classpath:db/sar/reset.sql")
  @Sql("classpath:db/sar/test_data.sql")
  override fun `SAR API should return expected data`() {
    super.`SAR API should return expected data`()
  }

  @Test
  @Sql("classpath:db/sar/reset.sql")
  @Sql("classpath:db/sar/test_data.sql")
  override fun `SAR report should render as expected`() {
    super.`SAR report should render as expected`()
  }
}
