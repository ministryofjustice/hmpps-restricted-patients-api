package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.PrisonerResult
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Component
class PrisonerSearchApiGateway(@Qualifier("prisonerSearchClientCreds") private val prisonerSearchClientCreds: WebClient) {

  fun searchByPrisonNumber(prisonNumber: String): List<PrisonerResult> = findByPrisonNumbers(listOf(prisonNumber))

  fun findByPrisonNumbers(prisonNumbers: List<String>): List<PrisonerResult> = prisonerSearchClientCreds
    .post()
    .uri {
      it.path("/prisoner-search/prisoner-numbers")
        .queryParam("responseFields", prisonerResultProperties)
        .build()
    }
    .bodyValue(
      mapOf("prisonerNumbers" to prisonNumbers),
    )
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<PrisonerResult>>() {})
    .block()!!

  companion object {
    // Get all property names from PrisonerResult class
    val prisonerResultProperties: List<String> by lazy {
      PrisonerResult::class.memberProperties
        .map { (it as KProperty1<*, *>).name }
    }
  }
}
