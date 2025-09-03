package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration
import kotlin.apply as kotlinApply

@Configuration
class WebClientConfig(
  @Value("\${prison.api.endpoint.url}") private val prisonApiUrl: String,
  @Value("\${prisoner.search.api.endpoint.url}") private val prisonerSearchApiUrl: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:20s}") val timeout: Duration,
) {

  @Bean
  fun prisonApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonApiUrl, healthTimeout)

  @Bean
  fun prisonerSearchHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonerSearchApiUrl, healthTimeout)

  @Bean
  @RequestScope
  fun prisonApiClientCreds(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManagerRequestScope(clientRegistrationRepository, authorizedClientRepository),
    registrationId = "restricted-patients-api",
    url = "$prisonApiUrl/api",
    timeout,
  )

  @Bean
  @RequestScope
  fun prisonerSearchClientCreds(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManagerRequestScope(clientRegistrationRepository, authorizedClientRepository),
    registrationId = "restricted-patients-api",
    url = prisonerSearchApiUrl,
    timeout,
  )

  @Bean
  fun prisonApiClientCredsAppScope(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "restricted-patients-api", url = "$prisonApiUrl/api", timeout)

  private fun authorizedClientManagerRequestScope(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
  ): OAuth2AuthorizedClientManager {
    val defaultClientCredentialsTokenResponseClient = DefaultClientCredentialsTokenResponseClient()
    val authentication: Authentication = SecurityContextHolder.getContext().authentication

    defaultClientCredentialsTokenResponseClient.setRequestEntityConverter { grantRequest: OAuth2ClientCredentialsGrantRequest? ->
      val converter = CustomOAuth2ClientCredentialsGrantRequestEntityConverter()
      converter.enhanceWithUsername(grantRequest, authentication.name)
    }

    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials { clientCredentialsGrantBuilder: OAuth2AuthorizedClientProviderBuilder.ClientCredentialsGrantBuilder ->
        clientCredentialsGrantBuilder.accessTokenResponseClient(
          defaultClientCredentialsTokenResponseClient,
        )
      }
      .build()
    return DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository).kotlinApply {
      setAuthorizedClientProvider(authorizedClientProvider)
    }
  }
}
