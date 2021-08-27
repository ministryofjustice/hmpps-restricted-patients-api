package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.PrisonerResult

@Component
class PrisonerSearchApiGateway(@Qualifier("prisonerSearchClientCreds") private val prisonerSearchClientCreds: WebClient) {

  fun searchByPrisonNumber(prisonNumber: String): List<PrisonerResult> = prisonerSearchClientCreds
    .post()
    .uri("/prisoner-search/prisoner-numbers")
    .bodyValue(
      mapOf(
        "prisonerNumbers" to listOf(prisonNumber),
      )
    )
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<PrisonerResult>>() {})
    .block()!!

  fun refreshPrisonerIndex(prisonerNumber: String): PrisonerResult = prisonerSearchClientCreds
    .put()
    .uri("/prisoner-index/index/prisoner/$prisonerNumber")
    .retrieve()
    .bodyToMono(PrisonerResult::class.java)
    .block()!!
}
