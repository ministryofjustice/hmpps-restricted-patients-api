@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup.MergeRestrictedPatientNotImplemented
import java.time.LocalDateTime
import java.util.Optional

class RestrictedPatientCleanupTest {
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val domainEventPublisher: DomainEventPublisher = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val restrictedPatientCleanup = RestrictedPatientCleanup(restrictedPatientsRepository, domainEventPublisher, telemetryClient)

  @Nested
  inner class ErrorHandling {
    @Test
    fun `fails gracefully when the restricted patient record no longer exists`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.empty())

      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")

      verifyNoInteractions(telemetryClient)
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
        "commentText",
      ).contains("A12345", "MDI", "HAZLWD", "MDI", LocalDateTime.parse("2020-10-10T20:00:01"), "test")
    }

    @Test
    fun `triggers a telemetry event`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")

      verify(telemetryClient).trackEvent(
        "restricted-patient-removed-cleanup",
        mapOf(
          "prisonerNumber" to "A12345",
        ),
        null,
      )
    }

    @Test
    fun `calls the domainEventPublisher service to raise a restricted patient removed event`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison("A12345")

      verify(domainEventPublisher).publishRestrictedPatientRemoved("A12345")
    }
  }

  @Nested
  inner class mergeRestrictedPatient {
    @Test
    fun `do nothing if removed prisoner doesn't exist`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.empty())
      restrictedPatientCleanup.mergeRestrictedPatient("A12345", "A23456")
      verifyNoInteractions(telemetryClient)
      verify(restrictedPatientsRepository).findById("A12345")
    }

    @Test
    fun `fails as not implemented on old prisoner`() {
      whenever(restrictedPatientsRepository.findById("A12345")).thenReturn(Optional.of(makeRestrictedPatient()))
      assertThatThrownBy {
        restrictedPatientCleanup.mergeRestrictedPatient("A12345", "A23456")
      }.isInstanceOf(MergeRestrictedPatientNotImplemented::class.java)
        .hasMessage("Merge not implemented. Patient A12345 was at hospital HAZLWD but record merged into A23456")

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `fails as not implemented on new prisoner`() {
      whenever(restrictedPatientsRepository.findById("A23456")).thenReturn(Optional.of(makeRestrictedPatient()))
      assertThatThrownBy {
        restrictedPatientCleanup.mergeRestrictedPatient("A12345", "A23456")
      }.isInstanceOf(MergeRestrictedPatientNotImplemented::class.java)
        .hasMessage("Merge not implemented. Patient A12345 is at hospital HAZLWD.  Record merged from A12345")

      verifyNoInteractions(telemetryClient)
    }
  }
}
