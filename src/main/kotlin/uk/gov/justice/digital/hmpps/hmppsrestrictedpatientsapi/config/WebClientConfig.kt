package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import UserContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
  @Value("\${prison.api.endpoint.url}") private val prisonApiUrl: String,
  @Value("\${prisoner.search.api.endpoint.url}") private val prisonerSearchApiUrl: String
) {

  @Bean
  fun prisonApiWithAuthWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl("$prisonApiUrl/api")
    .filter(addAuthHeaderFilterFunction())
    .build()

  @Bean
  fun prisonApiNoAuthWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonApiUrl)
    .build()

  @Bean
  fun prisonerSearchNoAuthWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonerSearchApiUrl)
    .build()

  @Bean
  fun prisonerSearchWithAuthWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonerSearchApiUrl)
    .filter(addAuthHeaderFilterFunction())
    .build()

  @Bean
  fun prisonApiClientCredsWebclient(
    authorizedClientManager: OAuth2AuthorizedClientManager?
  ): WebClient? {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("restricted-patients-api")

    val exchangeStrategies = ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()

    return WebClient.builder()
      .baseUrl("$prisonApiUrl/api")
      .apply(oauth2Client.oauth2Configuration())
      .exchangeStrategies(exchangeStrategies)
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction =
    ExchangeFilterFunction { request: ClientRequest?, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
        .build()
      next.exchange(filtered)
    }
}
