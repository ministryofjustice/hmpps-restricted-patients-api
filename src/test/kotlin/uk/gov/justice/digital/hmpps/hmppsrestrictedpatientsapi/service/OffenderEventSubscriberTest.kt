package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.GsonConfig
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeExternalMovementEventAsJson
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.ExternalPrisonerMovementMessage
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.OffenderEventSubscriber
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup

class OffenderEventSubscriberTest {
  private lateinit var offenderEventSubscriber: OffenderEventSubscriber

  private val restrictedPatientCleanup: RestrictedPatientCleanup = mock()

  @BeforeEach
  fun beforeEach() {
    offenderEventSubscriber = OffenderEventSubscriber(GsonConfig().gson(), restrictedPatientCleanup)
  }

  @Test
  fun `calls delete restricted patient when a new external movement going into a prison is received`() {
    offenderEventSubscriber.handleEvents(makeExternalMovementEventAsJson("A12345"))
    verify(restrictedPatientCleanup).deleteRestrictedPatientOnExternalMovementIntoPrison(
      ExternalPrisonerMovementMessage(
        bookingId = 100001,
        movementSeq = 3,
        offenderIdDisplay = "A12345",
        fromAgencyLocationId = "CRTTRN",
        toAgencyLocationId = "MDI",
        directionCode = "IN",
        movementType = "ADM"
      )
    )
  }
}
