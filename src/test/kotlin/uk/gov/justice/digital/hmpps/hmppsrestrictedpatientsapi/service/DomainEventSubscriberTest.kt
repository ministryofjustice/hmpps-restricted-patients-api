package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerReceiveEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventSubscriber
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup

class DomainEventSubscriberTest {
  private val restrictedPatientCleanup: RestrictedPatientCleanup = mock()
  private val domainEventSubscriber = DomainEventSubscriber(GsonAutoConfiguration().gson(GsonBuilder()), restrictedPatientCleanup)

  @Test
  fun `calls delete restricted patient when a new external movement going into a prison is received`() {
    domainEventSubscriber.handleEvents(makePrisonerReceiveEvent("A12345"))
    verify(restrictedPatientCleanup).deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")
  }
}
