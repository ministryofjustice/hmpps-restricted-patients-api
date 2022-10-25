package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import java.time.LocalDate

data class PrisonerResult(
  val prisonerNumber: String,
  val bookingId: Long,
  val conditionalReleaseDate: LocalDate?,
  val sentenceExpiryDate: LocalDate?,
  val recall: Boolean?,
  val indeterminateSentence: Boolean?, // will be null if no sentences set
  val legalStatus: String,
) {

  // I think this is due to dodgy sentence data, but some of the recalls have legal status of INDETERMINATE_SENTENCE
  // so try to only include ones that are actually recalls
  fun isRecallPastSentenceExpiry(now: LocalDate): Boolean =
    recall == true && (legalStatus == "SENTENCED" || legalStatus == "RECALL") && sentenceExpiryDate?.isBefore(now) ?: false

  fun isNotRecallPastConditionalRelease(now: LocalDate): Boolean =
    recall == false && legalStatus == "SENTENCED" && conditionalReleaseDate?.isBefore(now) ?: false
}
