package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class CommunityApiGateway(private val communityApiClientCreds: WebClient) {
  fun updateNomsNumber(crn: String, nomsNumber: String) =
    communityApiClientCreds
      .put()
      .uri("/offenders/crn/$crn/nomsNumber")
      .bodyValue(
        mapOf("nomsNumber" to nomsNumber)
      )
      .retrieve()
      .bodyToMono<CommunityDetail>()
      .block()!!
}

data class CommunityDetail(val croNumber: String, val pncNumber: String)
