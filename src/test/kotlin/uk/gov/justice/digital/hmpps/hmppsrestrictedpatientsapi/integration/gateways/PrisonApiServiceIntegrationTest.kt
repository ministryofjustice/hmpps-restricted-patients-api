package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.gateways

import com.github.tomakehurst.wiremock.client.WireMock
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiQueryService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@ActiveProfiles("test")
@SpringBootTest
@WithMockAuthUser
class PrisonApiServiceIntegrationTest {

  @Autowired
  private lateinit var prisonApiQueryService: PrisonApiQueryService

  @Nested
  inner class GetAgency {
    @Test
    fun `should return agency`() {
      prisonApiMockServer.stubGetAgency("MDI", "INST")

      val agency = prisonApiQueryService.getAgency("MDI")!!

      assertThat(agency.agencyId).isEqualTo("MDI")
      assertThat(agency.agencyType).isEqualTo("INST")
    }

    @Test
    fun `should return null if not found`() {
      prisonApiMockServer.stubGetAgencyNotFound("MDI")

      val agency = prisonApiQueryService.getAgency("MDI")

      assertThat(agency).isNull()
    }

    @Test
    fun `should throw exception if error`() {
      prisonApiMockServer.stubServerError(WireMock::get)

      assertThrows<WebClientResponseException.ServiceUnavailable> {
        prisonApiQueryService.getAgency("MDI")
      }
    }
  }

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @JvmField
    internal val oAuthMockServer = OAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
      oAuthMockServer.start()
      oAuthMockServer.stubGrantToken()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      oAuthMockServer.stop()
    }
  }
}
