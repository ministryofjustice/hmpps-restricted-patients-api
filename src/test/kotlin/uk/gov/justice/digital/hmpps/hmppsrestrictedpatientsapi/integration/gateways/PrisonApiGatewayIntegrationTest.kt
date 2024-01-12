package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.gateways

import com.github.tomakehurst.wiremock.client.WireMock
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.wiremock.PrisonApiMockServer

@ActiveProfiles("test")
@SpringBootTest(classes = [PrisonApiGateway::class, WebClientConfig::class, WebClientAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, SecurityAutoConfiguration::class])
@WithMockUser
class PrisonApiGatewayIntegrationTest {

  @Autowired
  protected lateinit var prisonApiGateway: PrisonApiGateway

  @Nested
  inner class GetAgency {
    @Test
    fun `should return agency`() {
      prisonApiMockServer.stubGetAgency("MDI")

      val agency = prisonApiGateway.getAgency("MDI")!!

      assertThat(agency.agencyId).isEqualTo("MDI")
      assertThat(agency.agencyType).isEqualTo("INST")
    }

    @Test
    fun `should return null if not found`() {
      prisonApiMockServer.stubGetAgencyNotFound("MDI")

      val agency = prisonApiGateway.getAgency("MDI")

      assertThat(agency).isNull()
    }

    @Test
    fun `should throw exception if error`() {
      prisonApiMockServer.stubServerError(WireMock::get)

      assertThrows<WebClientResponseException.ServiceUnavailable> {
        prisonApiGateway.getAgency("MDI")
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
