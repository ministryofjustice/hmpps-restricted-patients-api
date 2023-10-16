package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class CommunityApiGateway(@Qualifier("communityApiClientCreds") private val communityApiClientCreds: WebClient) {
  fun updateNomsNumber(crn: String, nomsNumber: String): Unit? =
    communityApiClientCreds
      .put()
      .uri("/offenders/crn/{crn}/nomsNumber", crn)
      .bodyValue(
        mapOf("nomsNumber" to nomsNumber),
      )
      .retrieve()
      .bodyToMono<Unit>()
      .block()
}
