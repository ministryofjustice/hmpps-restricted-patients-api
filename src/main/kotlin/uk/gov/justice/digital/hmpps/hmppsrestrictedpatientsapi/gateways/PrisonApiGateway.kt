package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.MovementResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse

@Service
class PrisonApiGateway(private val prisonApiClientCreds: WebClient) {
  fun dischargeToHospital(newRestrictedPatient: RestrictedPatient, noEventPropagation: Boolean = false): InmateDetail =
    prisonApiClientCreds
      .put()
      .uri("/offenders/{prisonerNumber}/discharge-to-hospital", newRestrictedPatient.prisonerNumber)
      .header("no-event-propagation", noEventPropagation.toString())
      .bodyValue(
        mapOf(
          "commentText" to newRestrictedPatient.commentText,
          "dischargeTime" to newRestrictedPatient.dischargeTime.toString(),
          "fromLocationId" to newRestrictedPatient.fromLocationId,
          "hospitalLocationCode" to newRestrictedPatient.hospitalLocationCode,
          "supportingPrisonId" to newRestrictedPatient.supportingPrisonId,
        ),
      )
      .retrieve()
      .bodyToMono<InmateDetail>()
      .block()!!

  fun getLatestMovements(offenderNo: String): List<MovementResponse> =
    prisonApiClientCreds
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

  fun getAgencyLocationsByType(type: String): List<Agency> = prisonApiClientCreds
    .get()
    .uri("/agencies/type/{type}", type)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<Agency>>() {})
    .block()!!

  fun createExternalMovement(createExternalMovement: CreateExternalMovement) {
    prisonApiClientCreds
      .post()
      .uri("/movements")
      .bodyValue(createExternalMovement)
      .retrieve()
      .bodyToMono(String::class.java)
      .block()
  }

  fun getOffenderBooking(offenderNo: String): OffenderBookingResponse? =
    prisonApiClientCreds
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

  private fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> =
    if (exception.statusCode == NOT_FOUND) Mono.empty() else Mono.error(exception)
}

class InmateDetail(val offenderNo: String)
