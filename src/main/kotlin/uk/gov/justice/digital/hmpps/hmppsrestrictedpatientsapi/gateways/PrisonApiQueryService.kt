package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.MovementResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse

@Service
class PrisonApiQueryService(private val prisonApiSystemClient: WebClient) {

  fun getLatestMovements(offenderNo: String): List<MovementResponse> = prisonApiSystemClient
    .post()
    .uri("/movements/offenders?latestOnly=true&allBookings=false")
    .bodyValue(
      listOf(
        offenderNo,
      ),
    )
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<MovementResponse>>() {})
    .block()!!

  fun getAgencyLocationsByType(type: String): List<Agency> = prisonApiSystemClient
    .get()
    .uri("/agencies/type/{type}", type)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<Agency>>() {})
    .block()!!

  fun getAgency(agencyId: String): Agency? = prisonApiSystemClient
    .get()
    .uri("/agencies/{agencyId}", agencyId)
    .retrieve()
    .bodyToMono<Agency>()
    .onErrorResume(WebClientResponseException.NotFound::class.java) {
      Mono.empty()
    }
    .block()

  fun getOffenderBooking(offenderNo: String): OffenderBookingResponse? = prisonApiSystemClient
    .get()
    .uri {
      it.path("/bookings/offenderNo/{offenderNo}")
        .queryParam("fullInfo", "true")
        .queryParam("extraInfo", "false")
        .queryParam("csraSummary", "false")
        .build(offenderNo)
    }
    .retrieve()
    .bodyToMono<OffenderBookingResponse>()
    .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
    .block()

  private fun <T : Any> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = if (exception.statusCode == NOT_FOUND) Mono.empty() else Mono.error(exception)
}

class InmateDetail(val offenderNo: String)
