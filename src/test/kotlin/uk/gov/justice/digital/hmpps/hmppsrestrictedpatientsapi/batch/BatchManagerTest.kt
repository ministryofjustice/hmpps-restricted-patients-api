package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.batch

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.batch.BatchType.PAST_DATE_CHECK
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.BatchReleaseDateRemoval

class BatchManagerTest {

  private val batchReleaseDateRemoval = mock<BatchReleaseDateRemoval>()
  private val event = mock<ContextRefreshedEvent>()
  private val context = mock<ConfigurableApplicationContext>()

  @BeforeEach
  fun setUp() {
    whenever(event.applicationContext).thenReturn(context)
  }

  @Test
  fun `should call the release date removal service`() {
    val batchManager = batchManager(PAST_DATE_CHECK)

    batchManager.onApplicationEvent(event)

    verify(batchReleaseDateRemoval).removeNonLifePrisonersPastRelevantDate()
    verify(context).close()
  }

  @Test
  fun `should not ignore unexpected error in the release date removal service`() {
    whenever(batchReleaseDateRemoval.removeNonLifePrisonersPastRelevantDate()).thenThrow(RuntimeException("error"))
    val batchManager = batchManager(PAST_DATE_CHECK)

    assertThrows<RuntimeException> {
      batchManager.onApplicationEvent(event)
    }

    verify(batchReleaseDateRemoval).removeNonLifePrisonersPastRelevantDate()
    verify(context, never()).close()
  }

  private fun batchManager(batchType: BatchType) = BatchManager(
    batchType,
    batchReleaseDateRemoval,
  )
}
