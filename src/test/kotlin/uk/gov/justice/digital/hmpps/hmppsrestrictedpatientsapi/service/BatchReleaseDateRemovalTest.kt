@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.BatchReleaseDateRemoval
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BatchReleaseDateRemovalTest {
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway = mock()
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val clock: Clock = mock()

  private val fixedClock =
    Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

  private val restrictedPatient = RestrictedPatient(
    prisonerNumber = "A12345",
    fromLocationId = "MDI",
    hospitalLocationCode = "HAZLWD",
    supportingPrisonId = "LEI",
    dischargeTime = LocalDateTime.now(),
    commentText = "test"
  )

  private val service = BatchReleaseDateRemoval(
    restrictedPatientsRepository,
    prisonerSearchApiGateway,
    telemetryClient,
    clock,
  )

  @BeforeEach
  fun beforeEach() {
    whenever(clock.instant()).thenReturn(fixedClock.instant())
    whenever(clock.zone).thenReturn(fixedClock.zone)
  }

  @Nested
  inner class removeNonLifePrisonersPastConditionalReleaseDate {
    @Test
    fun `does nothing if no prisoners matched`() {
      whenever(restrictedPatientsRepository.findAll()).thenReturn(listOf(restrictedPatient))
      whenever(prisonerSearchApiGateway.findByPrisonNumbers(any())).thenReturn(listOf(makePrisonerResult()))

      service.removeNonLifePrisonersPastConditionalReleaseDate()

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `calls prisoner offender search multiple times if more than 1000 restricted patients`() {
      whenever(restrictedPatientsRepository.findAll()).thenReturn(MutableList(3500) { restrictedPatient })
      whenever(prisonerSearchApiGateway.findByPrisonNumbers(any())).thenReturn(listOf(makePrisonerResult()))

      service.removeNonLifePrisonersPastConditionalReleaseDate()

      argumentCaptor<List<String>>().apply {
        verify(prisonerSearchApiGateway, times(4)).findByPrisonNumbers(capture())
        assertThat(allValues.map { it.size }).isEqualTo(listOf(1000, 1000, 1000, 500))
      }
    }

    @Test
    fun `tracks potentials removals`() {
      whenever(restrictedPatientsRepository.findAll()).thenReturn(listOf(restrictedPatient))
      whenever(prisonerSearchApiGateway.findByPrisonNumbers(any())).thenReturn(
        listOf(
          makePrisonerResult(
            prisonerNumber = "YESTERDAY_FALSE",
            conditionalReleaseDate = LocalDate.now().minusDays(1),
            indeterminateSentence = false,
          ),
          makePrisonerResult(
            prisonerNumber = "YESTERDAY_TRUE",
            conditionalReleaseDate = LocalDate.now().minusDays(1),
            indeterminateSentence = true,
          ),
          makePrisonerResult(
            prisonerNumber = "YESTERDAY_NOTSET",
            conditionalReleaseDate = LocalDate.now().minusDays(1),
            indeterminateSentence = null,
          ),
          makePrisonerResult(
            prisonerNumber = "TODAY_FALSE",
            conditionalReleaseDate = LocalDate.now(),
            indeterminateSentence = false,
          ),
        )
      )

      service.removeNonLifePrisonersPastConditionalReleaseDate()

      verify(telemetryClient).trackEvent("restricted-patient-batch-removal", mapOf("prisonerNumbers" to "YESTERDAY_FALSE"), null)
    }
  }
}