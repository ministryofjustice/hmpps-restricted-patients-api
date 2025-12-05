package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.microsoft.applicationinsights.TelemetryClient
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiQueryService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiUpdateService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonerSearchApiMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.BatchReleaseDateRemoval
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventPublisher
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@ExtendWith(OutputCaptureExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var flyway: Flyway

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  lateinit var restrictedPatientRepository: RestrictedPatientsRepository

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var batchReleaseDateRemoval: BatchReleaseDateRemoval

  @MockitoSpyBean
  lateinit var domainEventPublisher: DomainEventPublisher

  @MockitoSpyBean
  lateinit var telemetryClient: TelemetryClient

  @MockitoSpyBean
  lateinit var prisonApiQueryService: PrisonApiQueryService

  @MockitoSpyBean
  lateinit var prisonApiUpdateService: PrisonApiUpdateService

  val offenderEventQueue by lazy {
    hmppsQueueService.findByQueueId("offenderevents") ?: throw RuntimeException("Queue offenderevents not found")
  }
  val domainEventQueue by lazy {
    hmppsQueueService.findByQueueId("domainevents") ?: throw RuntimeException("Queue domainevents not found")
  }
  val testDomainEventQueue by lazy {
    hmppsQueueService.findByQueueId("testdomainevents") ?: throw RuntimeException("Queue testdomainevents not found")
  }

  @MockitoBean
  lateinit var clock: Clock

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

  @BeforeEach
  fun mockClock() {
    val fixedClock =
      Clock.fixed(
        LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault(),
      )
    whenever(clock.instant()).thenReturn(fixedClock.instant())
    whenever(clock.getZone()).thenReturn(fixedClock.getZone())
  }

  @AfterEach
  fun resetQueues() {
    offenderEventQueue.sqsClient.purgeQueue { it.queueUrl(offenderEventQueue.queueUrl) }
    offenderEventQueue.sqsClient.purgeQueue { it.queueUrl(offenderEventQueue.dlqUrl) }
    domainEventQueue.sqsClient.purgeQueue { it.queueUrl(domainEventQueue.queueUrl) }
    domainEventQueue.sqsClient.purgeQueue { it.queueUrl(domainEventQueue.dlqUrl) }
  }

  @AfterEach
  fun resetDb() {
    flyway.clean()
    flyway.migrate()
  }

  fun setHeaders(contentType: MediaType = APPLICATION_JSON, username: String? = "ITAG_USER", roles: List<String> = listOf()): (HttpHeaders) -> Unit = {
    jwtAuthHelper.setAuthorisationHeader(username = username, roles = roles)(it)
    it.contentType = contentType
  }

  fun loadResourceFile(fileName: String): String {
    val packageName = "uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration"
    return javaClass.getResource("/$packageName/$fileName").readText()
  }

  fun getRestrictedPatient(prisonerNumber: String = "A12345"): WebTestClient.RequestHeadersSpec<*> = webTestClient.get().uri("/restricted-patient/prison-number/$prisonerNumber")
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
          "supportingPrisonId" to supportingPrisonId,
        ),
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
          "supportingPrisonId" to supportingPrisonId,
        ),
      )
  }

  fun migrateInRestrictedPatientWebClient(
    prisonerNumber: String,
    hospitalLocationCode: String = "HAZLWD",
  ): WebTestClient.RequestHeadersSpec<*> {
    prisonApiMockServer.stubGetLatestMovementsReleased(prisonerNumber, hospitalLocationCode)
    prisonApiMockServer.stubDischargeToPrison(prisonerNumber)

    return webTestClient
      .post()
      .uri("/migrate-in-restricted-patient")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "offenderNo" to prisonerNumber,
          "hospitalLocationCode" to hospitalLocationCode,
        ),
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
        ),
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
        ),
      )
  }

  fun saveRestrictedPatient(
    prisonerNumber: String = "A1234BC",
    fromLocationId: String = "MDI",
    hospitalLocationCode: String = "HAZLWD",
    supportingPrisonId: String = "MDI",
    dischargeTime: LocalDateTime = LocalDateTime.now(clock).minusDays(1),
    commentText: String? = null,
  ) {
    restrictedPatientRepository.save(RestrictedPatient(prisonerNumber, fromLocationId, hospitalLocationCode, supportingPrisonId, dischargeTime, commentText))
  }
}
