package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerMergeEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventSubscriber
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup

class DomainEventSubscriberTest {
  private val restrictedPatientCleanup: RestrictedPatientCleanup = mock()
  private val domainEventSubscriber = DomainEventSubscriber(GsonAutoConfiguration().gson(GsonBuilder()), restrictedPatientCleanup)

  @Test
  fun `calls merge restricted patient when two prisoner records are merged`() {
    domainEventSubscriber.handleEvents(makePrisonerMergeEvent("A12345", "A23456"))
    verify(restrictedPatientCleanup).mergeRestrictedPatient("A12345", "A23456")
  }
}
