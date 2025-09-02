package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeOffenderMovementReceptionEvent
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.OffenderEventSubscriber
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup

@JsonTest
class OffenderEventSubscriberTest(@Autowired gson: Gson) {
  private val restrictedPatientCleanup: RestrictedPatientCleanup = mock()
  private val offenderEventSubscriber = OffenderEventSubscriber(gson, restrictedPatientCleanup)

  @Test
  fun `calls delete restricted patient when a new external movement going into a prison is received`() {
    offenderEventSubscriber.handleEvents(makeOffenderMovementReceptionEvent("A12345"))
    verify(restrictedPatientCleanup).deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")
  }
}
