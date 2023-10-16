package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime

@Service
class CaseNoteApiGateway(@Qualifier("caseNotesApiClientCreds")private val caseNoteApiClientCreds: WebClient) {
  fun createCaseNote(caseNoteRequest: CaseNoteRequest): Unit =
    with(caseNoteRequest) {
      caseNoteApiClientCreds
        .post()
        .uri("/case-notes/{offenderNumber}", offenderNumber)
        .bodyValue(
          mapOf(
            "locationId" to locationId,
            "type" to type,
            "subType" to subType,
            "occurrenceDateTime" to occurrenceDateTime,
            "text" to text,
          ),
        )
        .retrieve()
        .bodyToMono<Unit>()
        .block()
    }
}

class CaseNoteRequest(
  val offenderNumber: String,
  val locationId: String,
  val type: String,
  val subType: String,
  val occurrenceDateTime: LocalDateTime,
  val text: String,
)
