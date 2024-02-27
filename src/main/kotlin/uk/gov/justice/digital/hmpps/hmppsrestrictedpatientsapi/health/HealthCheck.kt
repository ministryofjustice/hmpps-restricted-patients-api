package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component
class PrisonApiHealthCheck(@Qualifier("prisonApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component
class PrisonerSearchApiHealthCheck(@Qualifier("prisonerSearchHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
