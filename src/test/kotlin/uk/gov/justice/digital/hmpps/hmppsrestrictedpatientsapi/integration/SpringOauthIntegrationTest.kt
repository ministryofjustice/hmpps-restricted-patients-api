package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.ActiveFlag

@ActiveProfiles("test")
class SpringOauthIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    prisonApiMockServer.stubAgencyLocationForPrisons()
    prisonApiMockServer.stubAgencyLocationForHospitals()
    prisonApiMockServer.stubDischargeToPrison(OFFENDER_ONE)
    prisonApiMockServer.stubOffenderBooking(OFFENDER_ONE, ActiveFlag.Y)

    prisonApiMockServer.stubDischargeToPrison(OFFENDER_TWO)
    prisonApiMockServer.stubOffenderBooking(OFFENDER_TWO, ActiveFlag.Y)
  }

  @Test
  fun `should request a new token for each request`() {
    webTestClient
      .post()
      .uri("/discharge-to-hospital")
      .headers(setHeaders(username = "USER1"))
      .bodyValue(getFormValues(OFFENDER_ONE))
      .exchange().expectStatus().is2xxSuccessful

    webTestClient
      .post()
      .uri("/discharge-to-hospital")
      .headers(setHeaders(username = "USER2"))
      .bodyValue(getFormValues(OFFENDER_TWO))
      .exchange().expectStatus().is2xxSuccessful

    oAuthMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/oauth/token"))
        .withRequestBody(WireMock.equalTo("grant_type=client_credentials&scope=write&username=USER1"))
    )

    oAuthMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/oauth/token"))
        .withRequestBody(WireMock.equalTo("grant_type=client_credentials&scope=write&username=USER2"))
    )
  }

  fun getFormValues(offenderNo: String): Map<String, Any> = mapOf(
    "offenderNo" to offenderNo,
    "commentText" to "hello",
    "dischargeTime" to "2010-10-10T21:00",
    "fromLocationId" to "MDI",
    "hospitalLocationCode" to "HAZLWD",
    "supportingPrisonId" to "MDI"
  )

  companion object {
    const val OFFENDER_ONE = "A12345"
    const val OFFENDER_TWO = "A12346"
  }
}
