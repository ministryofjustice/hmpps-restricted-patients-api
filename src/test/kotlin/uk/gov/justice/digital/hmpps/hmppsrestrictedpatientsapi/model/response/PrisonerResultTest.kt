package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerResult
import java.time.LocalDate

internal class PrisonerResultTest {

  @ParameterizedTest
  @MethodSource("getRecallData")
  fun isRecallPastSentenceExpiry(result: PrisonerResult, expected: Boolean) {
    assertThat(result.isRecallPastSentenceExpiry(testDate)).isEqualTo(expected)
  }

  @ParameterizedTest
  @MethodSource("getNonRecallData")
  fun isNotRecallPastConditionalRelease(result: PrisonerResult, expected: Boolean) {
    assertThat(result.isNotRecallPastConditionalRelease(testDate)).isEqualTo(expected)
  }

  companion object {
    val testDate: LocalDate = LocalDate.parse("2020-10-03")

    @JvmStatic
    fun getRecallData(): List<Arguments> =
      listOf(
        Arguments.of(makePrisonerResult(), false),
        Arguments.of(makePrisonerResult(recall = false, legalStatus = "RECALL", sentenceExpiryDate = testDate.minusDays(1)), false),
        Arguments.of(makePrisonerResult(recall = true, legalStatus = "UNSENTENCED", sentenceExpiryDate = testDate.minusDays(1)), false),
        Arguments.of(makePrisonerResult(recall = true, legalStatus = "RECALL", sentenceExpiryDate = testDate.minusDays(1)), true),
        Arguments.of(makePrisonerResult(recall = true, legalStatus = "SENTENCED", sentenceExpiryDate = testDate.minusDays(1)), true),
        Arguments.of(makePrisonerResult(recall = true, legalStatus = "RECALL", sentenceExpiryDate = testDate.plusDays(1)), false),
        Arguments.of(makePrisonerResult(recall = true, legalStatus = null, sentenceExpiryDate = testDate.plusDays(1)), false),
      )

    @JvmStatic
    fun getNonRecallData(): List<Arguments> =
      listOf(
        Arguments.of(makePrisonerResult(), false),
        Arguments.of(makePrisonerResult(recall = false, legalStatus = "RECALL", conditionalReleaseDate = testDate.minusDays(1)), false),
        Arguments.of(makePrisonerResult(recall = false, legalStatus = "SENTENCED", conditionalReleaseDate = testDate.minusDays(1)), true),
        Arguments.of(makePrisonerResult(recall = true, legalStatus = "SENTENCED", conditionalReleaseDate = testDate.minusDays(1)), false),
        Arguments.of(makePrisonerResult(recall = false, legalStatus = "SENTENCED", conditionalReleaseDate = testDate.plusDays(1)), false),
        Arguments.of(makePrisonerResult(recall = false, legalStatus = null, conditionalReleaseDate = testDate.plusDays(1)), false),
      )
  }
}
