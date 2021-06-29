package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import UserContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
  @Value("\${prison.api.endpoint.url}") private val prisonApiUrl: String,
  @Value("\${prisoner.search.api.endpoint.url}") private val prisonerSearchApiUrl: String
) {

  @Bean
  fun prisonAPiWebClientAuditable(builder: WebClient.Builder): WebClient = builder
    .baseUrl("$prisonApiUrl/api")
    .filter(addAuthHeaderFilterFunction())
    .build()

  @Bean
  fun prisonApiNoAuthToken(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonApiUrl)
    .build()

  @Bean
  fun prisonerSearchApiUrlNoAuthToken(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonerSearchApiUrl)
    .build()

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction =
    ExchangeFilterFunction { request: ClientRequest?, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
        .build()
      next.exchange(filtered)
    }
}
