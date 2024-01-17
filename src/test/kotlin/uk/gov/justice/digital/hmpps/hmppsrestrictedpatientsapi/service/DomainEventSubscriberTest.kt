package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeOutboundPrisonerSearchReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerMergeEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventSubscriber
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientEventService

class DomainEventSubscriberTest {
  private val restrictedPatientCleanup: RestrictedPatientCleanup = mock()
  private val restrictedPatientsEventService: RestrictedPatientEventService = mock()
  private val domainEventSubscriber = DomainEventSubscriber(GsonAutoConfiguration().gson(GsonBuilder()), restrictedPatientCleanup, restrictedPatientsEventService)

  @Test
  fun `calls merge restricted patient when two prisoner records are merged`() {
    domainEventSubscriber.handleEvents(makePrisonerMergeEvent("A12345", "A23456"))
    verify(restrictedPatientCleanup).mergeRestrictedPatient("A12345", "A23456")
  }

  @Test
  fun `calls prisoner released when prisoner released message received`() {
    domainEventSubscriber.handleEvents(makeOutboundPrisonerSearchReleasedEvent("A12345"))
    verify(restrictedPatientsEventService).prisonerReleased("A12345")
  }
}
