package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
  @Value("\${prison.api.endpoint.url}") private val prisonApiUrl: String,
  @Value("\${prisoner.search.api.endpoint.url}") private val prisonerSearchApiUrl: String,
  @Value("\${community.api.endpoint.url}") private val communityApiUrl: String,
) {

  @Bean
  fun prisonApiNoAuthWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonApiUrl)
    .build()

  @Bean
  fun prisonerSearchNoAuthWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonerSearchApiUrl)
    .build()

  @Bean
  @RequestScope
  @Profile("!app-scope")
  fun prisonApiClientCreds(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
  ): WebClient? = getClientCredsWebClient(
    "$prisonApiUrl/api",
    authorizedClientManagerRequestScope(clientRegistrationRepository, authorizedClientRepository)
  )

  @Bean
  @RequestScope
  @Profile("!app-scope")
  fun prisonerSearchClientCreds(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
  ): WebClient? = getClientCredsWebClient(
    prisonerSearchApiUrl,
    authorizedClientManagerRequestScope(clientRegistrationRepository, authorizedClientRepository)
  )

  @Bean
  @RequestScope
  @Profile("!app-scope")
  fun communityApiClientCreds(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
  ): WebClient? = getClientCredsWebClient(
    "$communityApiUrl/secure",
    authorizedClientManagerRequestScope(clientRegistrationRepository, authorizedClientRepository)
  )

  @Bean
  @Qualifier("prisonApiClientCreds")
  @Profile("app-scope")
  fun prisonApiClientCredsAppScope(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): WebClient? = getClientCredsWebClient(
    "$prisonApiUrl/api",
    authorizedClientManagerAppScope(clientRegistrationRepository, oAuth2AuthorizedClientService)
  )

  @Bean
  @Qualifier("prisonerSearchClientCreds")
  @Profile("app-scope")
  fun prisonerSearchClientCredsAppScope(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): WebClient? = getClientCredsWebClient(
    prisonerSearchApiUrl,
    authorizedClientManagerAppScope(clientRegistrationRepository, oAuth2AuthorizedClientService)
  )

  @Bean
  @Qualifier("communityApiClientCreds")
  @Profile("app-scope")
  fun communityApiClientCredsAppScope(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
  ): WebClient? = getClientCredsWebClient(
    "$communityApiUrl/secure",
    authorizedClientManagerRequestScope(clientRegistrationRepository, authorizedClientRepository)
  )

  private fun getClientCredsWebClient(
    url: String,
    authorizedClientManager: OAuth2AuthorizedClientManager?
  ): WebClient? {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("restricted-patients-api")

    val exchangeStrategies = ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()

    return WebClient.builder()
      .baseUrl(url)
      .apply(oauth2Client.oauth2Configuration())
      .exchangeStrategies(exchangeStrategies)
      .build()
  }

  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun authorizedClientManagerRequestScope(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository
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
          defaultClientCredentialsTokenResponseClient
        )
      }
      .build()
    val authorizedClientManager =
      DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
