package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.batch

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.BatchReleaseDateRemoval

enum class BatchType {
  PAST_DATE_CHECK,
}

@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
@Service
class BatchManager(
  @Value($$"${batch.type}") private val batchType: BatchType,
  private val batchReleaseDateRemoval: BatchReleaseDateRemoval,
) {

  @EventListener
  fun onApplicationEvent(event: ContextRefreshedEvent) = runBatchJob(batchType).also { event.closeApplication() }

  @WithSpan
  fun runBatchJob(@SpanAttribute batchType: BatchType) = when (batchType) {
    BatchType.PAST_DATE_CHECK -> batchReleaseDateRemoval.removeNonLifePrisonersPastRelevantDate()
  }

  private fun ContextRefreshedEvent.closeApplication() = (this.applicationContext as ConfigurableApplicationContext).close()
}
