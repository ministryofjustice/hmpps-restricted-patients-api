package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup
import java.time.LocalDateTime
import java.util.Optional

class RestrictedPatientCleanupTest {
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private lateinit var restrictedPatientCleanup: RestrictedPatientCleanup

  @BeforeEach
  fun beforeEach() {
    restrictedPatientCleanup = RestrictedPatientCleanup(restrictedPatientsRepository, telemetryClient)
  }

  @Nested
  inner class ErrorHandling {
    @Test
    fun `fails gracefully when the restricted patient record no longer exists`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.empty())

      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")

      verify(telemetryClient, never()).trackEvent(anyString(), any(), any())
    }
  }

  @Nested
  inner class WithValidBookingAndRestrictedPatient {
    @BeforeEach
    fun beforeEach() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.of(makeRestrictedPatient()))
    }

    @Test
    fun `loads the restricted patient by prisoner number`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")

      verify(restrictedPatientsRepository).findById("A12345")
    }

    @Test
    fun `loads the restricted patient then deletes it`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")

      val argumentCaptor = ArgumentCaptor.forClass(RestrictedPatient::class.java)

      verify(restrictedPatientsRepository).delete(argumentCaptor.capture())

      assertThat(argumentCaptor.value).extracting(
        "prisonerNumber",
        "fromLocationId",
        "hospitalLocationCode",
        "supportingPrisonId",
        "dischargeTime",
        "commentText"
      ).contains("A12345", "MDI", "HAZLWD", "MDI", LocalDateTime.parse("2020-10-10T20:00:01"), "test")
    }

    @Test
    fun `triggers a telemetry event`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")

      verify(telemetryClient).trackEvent(
        "restricted-patient-removed-on-prison-offender-events.prisoner.receive",
        mapOf(
          "prisonerNumber" to "A12345",
        ),
        null
      )
    }
  }
}
