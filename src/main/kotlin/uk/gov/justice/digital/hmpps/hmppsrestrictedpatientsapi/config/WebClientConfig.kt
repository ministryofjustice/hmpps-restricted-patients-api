package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import uk.gov.justice.hmpps.kotlin.auth.usernameAwareTokenRequestOAuth2AuthorizedClientManager
import java.time.Duration

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
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    usernameAwareTokenRequestOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    ),
    registrationId = "restricted-patients-api",
    url = "$prisonApiUrl/api",
    timeout,
  )

  @Bean
  @RequestScope
  @ConditionalOnProperty(name = ["batch.enabled"], havingValue = "false", matchIfMissing = true)
  fun prisonerSearchClientCreds(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    usernameAwareTokenRequestOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    ),
    registrationId = "restricted-patients-api",
    url = prisonerSearchApiUrl,
    timeout,
  )

  @Bean
  fun prisonApiClientCredsAppScope(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "restricted-patients-api", url = "$prisonApiUrl/api", timeout)

  @Bean("prisonerSearchClientCreds")
  @ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true", matchIfMissing = false)
  fun prisonerSearchClientCredsAppScope(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "restricted-patients-api", url = prisonerSearchApiUrl, timeout)
}
