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
import java.time.LocalDate

@Service
class PrisonApiGateway(private val prisonApiClientCreds: WebClient) {
  fun dischargeToHospital(newRestrictedPatient: RestrictedPatient): InmateDetail =
    prisonApiClientCreds
      .put()
      .uri("/offenders/${newRestrictedPatient.prisonerNumber}/discharge-to-hospital")
      .bodyValue(
        mapOf(
          "commentText" to newRestrictedPatient.commentText,
          "dischargeTime" to newRestrictedPatient.dischargeTime.toString(),
          "fromLocationId" to newRestrictedPatient.fromLocationId,
          "hospitalLocationCode" to newRestrictedPatient.hospitalLocationCode,
          "supportingPrisonId" to newRestrictedPatient.supportingPrisonId
        )
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
          offenderNo
        )
      )
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<MovementResponse>>() {})
      .block()!!

  fun getAgencyLocationsByType(type: String): List<Agency> = prisonApiClientCreds
    .get()
    .uri("/agencies/type/$type")
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
    if (exception.rawStatusCode == NOT_FOUND.value()) Mono.empty() else Mono.error(exception)

  fun createPrisoner(
    surname: String,
    firstName: String,
    middleNames: String?,
    gender: String,
    dateOfBirth: LocalDate,
    croNumber: String?,
    pncNumber: String?,
  ): InmateDetail =
    prisonApiClientCreds
      .post()
      .uri("/offenders")
      .bodyValue(
        mapOf(
          "lastName" to surname,
          "firstName" to firstName,
          "middleName1" to middleNames?.split(" ")?.firstOrNull(),
          "middleName2" to middleNames?.split(" ")?.drop(1)?.joinToString(" "),
          "gender" to gender,
          "dateOfBirth" to dateOfBirth.toString(),
          "croNumber" to croNumber,
          "pnvNumber" to pncNumber,
        )
      )
      .retrieve()
      .bodyToMono<InmateDetail>()
      .block()!!
}

class InmateDetail(val offenderNo: String)
