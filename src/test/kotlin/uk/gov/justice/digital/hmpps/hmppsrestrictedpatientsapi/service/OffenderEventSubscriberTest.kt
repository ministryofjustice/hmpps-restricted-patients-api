package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeOffenderMovementReceptionEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.OffenderEventSubscriber
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup

class OffenderEventSubscriberTest {
  private val restrictedPatientCleanup: RestrictedPatientCleanup = mock()
  private val offenderEventSubscriber = OffenderEventSubscriber(GsonAutoConfiguration().gson(GsonBuilder()), restrictedPatientCleanup)

  @Test
  @Disabled
  fun `calls delete restricted patient when a new external movement going into a prison is received`() {
    offenderEventSubscriber.handleEvents(makeOffenderMovementReceptionEvent("A12345"))
    verify(restrictedPatientCleanup).deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")
  }
}
