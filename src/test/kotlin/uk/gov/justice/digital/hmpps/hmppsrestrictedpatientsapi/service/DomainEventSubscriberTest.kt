package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeOutboundPrisonerSearchReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerMergeEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventSubscriber
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientEventService

@JsonTest
class DomainEventSubscriberTest(@Autowired gson: Gson) {
  private val restrictedPatientCleanup: RestrictedPatientCleanup = mock()
  private val restrictedPatientsEventService: RestrictedPatientEventService = mock()
  private val domainEventSubscriber = DomainEventSubscriber(gson, restrictedPatientCleanup, restrictedPatientsEventService)

  @Test
  fun `calls merge restricted patient when two prisoner records are merged`() {
    domainEventSubscriber.handleEvents(makePrisonerMergeEvent("A12345", "A23456"))
    verify(restrictedPatientCleanup).mergeRestrictedPatient("A12345", "A23456")
  }

  @Test
  fun `calls prisoner released when prisoner released message received`() {
    domainEventSubscriber.handleEvents(makeOutboundPrisonerSearchReleasedEvent("A12345"))
    verify(restrictedPatientsEventService).prisonerReleased("A12345", "RELEASED_TO_HOSPITAL")
  }
}
