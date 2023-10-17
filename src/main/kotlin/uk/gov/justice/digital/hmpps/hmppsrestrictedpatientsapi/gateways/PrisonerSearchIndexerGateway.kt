package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.PrisonerResult

@Component
class PrisonerSearchIndexerGateway(@Qualifier("prisonerSearchIndexerClientCreds") private val prisonerSearchIndexerClientCreds: WebClient) {
  fun refreshPrisonerIndex(prisonerNumber: String): PrisonerResult = prisonerSearchIndexerClientCreds
    .put()
    .uri("/maintain-index/index-prisoner/{prisonerNumber}", prisonerNumber)
    .retrieve()
    .bodyToMono(PrisonerResult::class.java)
    .block()!!
}
