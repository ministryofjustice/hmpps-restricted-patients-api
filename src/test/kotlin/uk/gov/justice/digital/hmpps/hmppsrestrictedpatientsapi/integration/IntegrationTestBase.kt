package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonerSearchApiMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.UnknownPatientService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var flyway: Flyway

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var restrictedPatientRepository: RestrictedPatientsRepository

  @MockBean
  lateinit var clock: Clock

  @SpyBean
  lateinit var unknownPatientsService: UnknownPatientService

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @JvmField
    internal val prisonerSearchApiMockServer = PrisonerSearchApiMockServer()

    @JvmField
    internal val communityApiMockServer = CommunityApiMockServer()

    @JvmField
    internal val oAuthMockServer = OAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      oAuthMockServer.start()
      oAuthMockServer.stubGrantToken()

      prisonApiMockServer.start()
      prisonerSearchApiMockServer.start()
      communityApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      prisonerSearchApiMockServer.stop()
      communityApiMockServer.stop()
      oAuthMockServer.stop()
    }
  }

  @BeforeEach
  fun mockClock() {
    val fixedClock =
      Clock.fixed(
        LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
      )
    whenever(clock.instant()).thenReturn(fixedClock.instant())
    whenever(clock.getZone()).thenReturn(fixedClock.getZone())
  }

  @AfterEach
  fun resetDb() {
    flyway.clean()
    flyway.migrate()
  }

  fun setHeaders(contentType: MediaType = MediaType.APPLICATION_JSON, username: String? = "ITAG_USER", roles: List<String> = listOf()): (HttpHeaders) -> Unit = {
    it.setBearerAuth(jwtAuthHelper.createJwt(subject = username, roles = roles))
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
    commentText: String = "Prisoner was released to hospital",
    fromLocationId: String = "MDI",
    hospitalLocationCode: String = "HAZLWD",
    supportingPrisonId: String = "MDI",
    activeFlag: Boolean = true,
  ): WebTestClient.RequestHeadersSpec<*> {
    stubDischargePrisoner(prisonerNumber, activeFlag)

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

  fun stubDischargePrisoner(prisonerNumber: String, activeFlag: Boolean = true) {
    prisonApiMockServer.stubAgencyLocationForPrisons()
    prisonApiMockServer.stubAgencyLocationForHospitals()
    prisonApiMockServer.stubOffenderBooking(prisonerNumber, activeFlag)
    prisonApiMockServer.stubDischargeToPrison(prisonerNumber)
  }

  fun dischargePrisonerWebClientErrors(
    prisonerNumber: String,
    commentText: String = "Prisoner was released to hospital",
    fromLocationId: String = "MDI",
    hospitalLocationCode: String = "HAZLWD",
    supportingPrisonId: String = "MDI",
  ): WebTestClient.RequestHeadersSpec<*> {
    prisonApiMockServer.stubServerError(WireMock::get)

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
    prisonApiMockServer.stubGetLatestMovementsReleased(prisonerNumber, hospitalLocationCode)
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

  fun migrateInRestrictedPatientWebClientNotReleased(
    prisonerNumber: String,
    hospitalLocationCode: String = "HAZLWD",
  ): WebTestClient.RequestHeadersSpec<*> {
    prisonApiMockServer.stubGetLatestMovementsAdmitted(prisonerNumber)

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

  fun migrateInRestrictedPatientWebClientError(
    prisonerNumber: String,
    hospitalLocationCode: String = "HAZLWD",
  ): WebTestClient.RequestHeadersSpec<*> {
    prisonApiMockServer.stubServerError(WireMock::post)

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

  fun processUnknownPatientsWebClient(
    csvData: List<String> = listOf(),
    headers: (HttpHeaders) -> Unit = {},
  ): WebTestClient.RequestHeadersSpec<*> {
    return webTestClient
      .post()
      .uri("/process-unknown-patients")
      .contentType(APPLICATION_JSON)
      .accept(APPLICATION_JSON)
      .headers(headers)
      .bodyValue(jacksonObjectMapper().writeValueAsString(csvData))
  }

  fun saveRestrictedPatient(
    prisonerNumber: String = "A1234BC",
    fromLocationId: String = "MDI",
    hospitalLocationCode: String = "HAZLWD",
    supportingPrisonId: String = "MDI",
    dischargeTime: LocalDateTime = LocalDateTime.now(clock).minusDays(1),
    commentText: String? = null
  ) {
    restrictedPatientRepository.save(RestrictedPatient(prisonerNumber, fromLocationId, hospitalLocationCode, supportingPrisonId, dischargeTime, commentText))
  }
}
