package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeExternalPrisonerMovementMessage
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientCleanup
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class RestrictedPatientCleanupTest {
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private lateinit var restrictedPatientCleanup: RestrictedPatientCleanup

  @BeforeEach
  fun beforeEach() {
    restrictedPatientCleanup = RestrictedPatientCleanup(prisonApiGateway, restrictedPatientsRepository, telemetryClient)
  }

  @Nested
  inner class ErrorHandling {
    @Test
    fun `fails gracefully when booking does not exists`() {
      whenever(prisonApiGateway.getOffenderBooking(1L)).thenThrow(WebClientResponseException::class.java)

      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(makeExternalPrisonerMovementMessage())

      verify(restrictedPatientsRepository, never()).findByPrisonerNumber(anyString())
      verify(telemetryClient, never()).trackEvent(anyString(), any(), any())
    }

    @Test
    fun `fails gracefully when the restricted patient record no longer exists`() {
      whenever(prisonApiGateway.getOffenderBooking(1L)).thenReturn(OffenderBookingResponse(1L, "A12345"))
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenThrow(EntityNotFoundException::class.java)

      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(makeExternalPrisonerMovementMessage())

      verify(telemetryClient, never()).trackEvent(anyString(), any(), any())
    }

    @Test
    fun `skips if the receiving agency is not a valid prison`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(makeExternalPrisonerMovementMessage())

      verify(restrictedPatientsRepository, never()).findByPrisonerNumber(anyString())
      verify(telemetryClient, never()).trackEvent(anyString(), any(), any())
    }
  }

  @Nested
  inner class WithValidBookingAndRestrictedPatient {
    @BeforeEach
    fun beforeEach() {
      whenever(prisonApiGateway.getOffenderBooking(1L)).thenReturn(OffenderBookingResponse(1L, "A12345"))
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(makeRestrictedPatient())
      whenever(prisonApiGateway.getAgencyById("MDI")).thenReturn(
        Agency(
          agencyId = "MDI",
          active = true,
          agencyType = "INST"
        )
      )
    }

    @Test
    fun `skips event if movement does not move the prisoner back into prison`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(
        makeExternalPrisonerMovementMessage(
          directionCode = "OUT",
          toAgencyLocationId = "OUT"
        )
      )

      verify(restrictedPatientsRepository, never()).findByPrisonerNumber(anyString())
      verify(telemetryClient, never()).trackEvent(anyString(), any(), any())
    }

    @Test
    fun `makes a call to prison api to get the prisoner number`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(makeExternalPrisonerMovementMessage())

      verify(prisonApiGateway, times(1)).getOffenderBooking(1L)
    }

    @Test
    fun `loads the restricted patient by prisoner number`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(makeExternalPrisonerMovementMessage())

      verify(restrictedPatientsRepository).findByPrisonerNumber("A12345")
    }

    @Test
    fun `loads the restricted patient then deletes it`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(makeExternalPrisonerMovementMessage())

      val argumentCaptor = ArgumentCaptor.forClass(RestrictedPatient::class.java)

      verify(restrictedPatientsRepository).delete(argumentCaptor.capture())

      assertThat(argumentCaptor.value).extracting(
        "id",
        "prisonerNumber",
        "fromLocationId",
        "hospitalLocationCode",
        "supportingPrisonId",
        "dischargeTime",
        "commentText"
      ).contains(1L, "A12345", "MDI", "HAZLWD", "MDI", LocalDateTime.parse("2020-10-10T20:00:01"), "test")
    }

    @Test
    fun `triggers a telemetry event`() {
      restrictedPatientCleanup.deleteRestrictedPatientOnExternalMovementIntoPrison(makeExternalPrisonerMovementMessage())

      verify(telemetryClient).trackEvent(
        "restricted-patient-removed-on-external-movement-in",
        mapOf(
          "prisonerNumber" to "A12345",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI",
          "dischargeTime" to LocalDateTime.parse("2020-10-10T20:00:01").toString(),
        ),
        null
      )
    }
  }
}
