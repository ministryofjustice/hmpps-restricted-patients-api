package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonerSearchApiMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var flyway: Flyway

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @JvmField
    internal val prisonerSearchApiMockServer = PrisonerSearchApiMockServer()

    @JvmField
    internal val oAuthMockServer = OAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      oAuthMockServer.start()
      oAuthMockServer.stubGrantToken()

      prisonApiMockServer.start()
      prisonerSearchApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      prisonerSearchApiMockServer.stop()
      oAuthMockServer.stop()
    }
  }

  @AfterEach
  fun resetDb() {
    flyway.clean()
    flyway.migrate()
  }

  fun setHeaders(contentType: MediaType = MediaType.APPLICATION_JSON, username: String? = "ITAG_USER"): (HttpHeaders) -> Unit = {
    it.setBearerAuth(jwtAuthHelper.createJwt(subject = username))
    it.contentType = contentType
  }

  fun loadResourceFile(fileName: String): String {
    val packageName = "uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration"
    return javaClass.getResource("/$packageName/$fileName").readText()
  }

  fun getRestrictedPatient(prisonerNumber: String = "A12345"): WebTestClient.RequestHeadersSpec<*> =
    webTestClient.get().uri("/restricted-patient/prison-number/$prisonerNumber")
      .headers(setHeaders())

  fun dischargePrisonerWebClient(
    prisonerNumber: String,
    commentText: String = "Prisoner was released on bail",
    fromLocationId: String = "MDI",
    hospitalLocationCode: String = "HAZLWD",
    supportingPrisonId: String = "MDI"
  ): WebTestClient.RequestHeadersSpec<*> {
    prisonApiMockServer.stubAgencyLocationForPrisons()
    prisonApiMockServer.stubAgencyLocationForHospitals()
    prisonApiMockServer.stubDischargeToPrison(prisonerNumber)
    prisonerSearchApiMockServer.stubSearchByPrisonNumber(prisonerNumber)

    return webTestClient
      .post()
      .uri("/discharge-to-hospital")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "offenderNo" to prisonerNumber,
          "commentText" to commentText,
          "fromLocationId" to fromLocationId,
          "hospitalLocationCode" to hospitalLocationCode,
          "supportingPrisonId" to supportingPrisonId
        )
      )
  }

  fun migrateInRestrictedPatientWebClient(
    prisonerNumber: String,
    hospitalLocationCode: String = "HAZLWD",
  ): WebTestClient.RequestHeadersSpec<*> {
    prisonApiMockServer.stubGetLatestMovements(prisonerNumber, hospitalLocationCode)
    prisonApiMockServer.stubDischargeToPrison(prisonerNumber)
    prisonerSearchApiMockServer.stubRefreshIndex(prisonerNumber)

    return webTestClient
      .post()
      .uri("/migrate-in-restricted-patient")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "offenderNo" to prisonerNumber,
          "hospitalLocationCode" to hospitalLocationCode,
        )
      )
  }
}
