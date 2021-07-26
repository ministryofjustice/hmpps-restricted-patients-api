package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.HOSPITAL
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.PRISON
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeToHospitalResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions.NoResultsReturnedException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.PrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

class RestrictedPatientServiceTest {

  private val prisonApiGateway: PrisonApiGateway = mock()
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway = mock()
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val clock: Clock = mock()

  private lateinit var service: RestrictedPatientsService

  @BeforeEach
  fun beforeEach() {
    val fixedClock =
      Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
    whenever(clock.instant()).thenReturn(fixedClock.instant())
    whenever(clock.getZone()).thenReturn(fixedClock.getZone())

    service = RestrictedPatientsService(
      prisonApiGateway,
      prisonerSearchApiGateway,
      restrictedPatientsRepository,
      telemetryClient,
      clock
    )
  }

  @Nested
  inner class RemovesRestrictedPatient {
    @Test
    fun `checks to see if there is a restricted patient associated with the prisoner number`() {
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(
        makeRestrictedPatient()
      )
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      verify(restrictedPatientsRepository).findByPrisonerNumber("A12345")
    }

    @Test
    fun `handles no restricted patient record exists`() {
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(null)

      val exception = Assertions.assertThrows(EntityNotFoundException::class.java) {
        service.removeRestrictedPatient("A12345")
      }

      assertThat(exception.message).isEqualTo("No restricted patient record found for prison number A12345")
    }

    @Test
    fun `makes a call to prisoner offender search`() {
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(
        makeRestrictedPatient()
      )

      service.removeRestrictedPatient("A12345")

      verify(prisonerSearchApiGateway).searchByPrisonNumber("A12345")
    }

    @Test
    fun `handles no prisoner offender search results returned`() {
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(
        makeRestrictedPatient()
      )
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(emptyList())

      val exception = Assertions.assertThrows(NoResultsReturnedException::class.java) {
        service.removeRestrictedPatient("A12345")
      }

      assertThat(exception.message).isEqualTo("No prisoner search results returned for A12345")
    }

    @Test
    fun `makes a call to prison api to create an external movement form hospital to community`() {
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(
        makeRestrictedPatient()
      )
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      verify(prisonApiGateway).createExternalMovement(
        CreateExternalMovement(
          bookingId = 1L,
          fromAgencyId = "HAZLWD",
          toAgencyId = "OUT",
          movementTime = LocalDateTime.now(clock),
          movementType = "REL",
          movementReason = "CR",
          directionCode = "OUT"
        )
      )
    }

    @Test
    fun `remove restricted patient`() {
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(makeRestrictedPatient())
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

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
      whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(makeRestrictedPatient())
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      verify(telemetryClient).trackEvent(
        "restricted-patient-removed",
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

  @Nested
  inner class DischargeToHospital {
    @Nested
    inner class Failures {
      @Test
      fun `will not discharge a prisoner with the wrong legal status`() {
        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          listOf(PrisonerResult(prisonerNumber = "A12345", legalStatus = LegalStatus.UNKNOWN, bookingId = 1L))
        )

        Assertions.assertThrows(ValidationException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }

        verify(prisonApiGateway, never()).dischargeToHospital(any())
      }

      @Test
      fun `throws not found exception`() {
        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          emptyList()
        )

        Assertions.assertThrows(NoResultsReturnedException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }
      }
    }

    @Nested
    inner class TestAllLegalStatuses {
      @ParameterizedTest
      @EnumSource(
        value = LegalStatus::class,
        names = ["INDETERMINATE_SENTENCE", "RECALL", "SENTENCED", "CONVICTED_UNSENTENCED", "IMMIGRATION_DETAINEE"]
      )
      fun `discharge an prisoner to hospital when they have the correct legal status`(status: LegalStatus) {
        whenever(restrictedPatientsRepository.save(any())).thenReturn(makeRestrictedPatient())
        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          listOf(PrisonerResult(prisonerNumber = "A12345", legalStatus = status, bookingId = 1L))
        )

        service.dischargeToHospital(makeDischargeRequest())

        verify(prisonApiGateway).dischargeToHospital(any())
      }
    }

    @Nested
    inner class SuccessfulDischargeToHospital {
      @BeforeEach
      fun beforeEach() {
        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          listOf(PrisonerResult(prisonerNumber = "A12345", legalStatus = LegalStatus.SENTENCED, bookingId = 1L))
        )
        whenever(restrictedPatientsRepository.save(any())).thenReturn(makeRestrictedPatient())
      }

      @Test
      fun `make a call to prison api to discharge a prisoner to hospital`() {
        whenever(prisonApiGateway.dischargeToHospital(any())).thenReturn(makeDischargeToHospitalResponse())

        val response = service.dischargeToHospital(makeDischargeRequest())

        verify(prisonApiGateway).dischargeToHospital(
          DischargeToHospitalRequest(
            offenderNo = "A12345",
            commentText = "test",
            dischargeTime = LocalDateTime.parse("2020-10-10T20:00:01"),
            fromLocationId = "MDI",
            hospitalLocationCode = "HAZLWD",
            supportingPrisonId = "MDI"
          )
        )

        assertThat(response.fromLocation).isEqualTo(PRISON)
        assertThat(response.supportingPrison).isEqualTo(PRISON)
        assertThat(response.hospitalLocation).isEqualTo(HOSPITAL)

        assertThat(response)
          .extracting(
            "id",
            "prisonerNumber",
            "dischargeTime",
            "commentText"
          )
          .contains(1L, "A12345", LocalDateTime.parse("2020-10-10T20:00:01"), "test")
      }

      @Test
      fun `calls save with the correct parameters`() {
        val argumentCaptor = ArgumentCaptor.forClass(RestrictedPatient::class.java)

        service.dischargeToHospital(makeDischargeRequest())

        verify(restrictedPatientsRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.value).extracting(
          "fromLocationId",
          "supportingPrisonId",
          "hospitalLocationCode",
          "commentText",
          "dischargeTime"
        ).contains("MDI", "MDI", "HAZLWD", "test", LocalDateTime.parse("2020-10-10T20:00:01"))
      }
    }

    @Nested
    inner class GetRestrictedPatient {
      @Test
      fun `throws entity not found`() {
        whenever(restrictedPatientsRepository.findByPrisonerNumber(any())).thenReturn(null)

        Assertions.assertThrows(EntityNotFoundException::class.java) {
          service.getRestrictedPatient("A12345")
        }
      }

      @Test
      fun `by prison number`() {
        whenever(prisonApiGateway.getAgencyLocationsByType("HSHOSP")).thenReturn(listOf(HOSPITAL))
        whenever(prisonApiGateway.getAgencyLocationsByType("INST")).thenReturn(listOf(PRISON))
        whenever(restrictedPatientsRepository.findByPrisonerNumber(anyString())).thenReturn(
          makeRestrictedPatient()
        )

        val restrictedPatient = service.getRestrictedPatient("A12345")

        assertThat(restrictedPatient.fromLocation).isEqualTo(PRISON)
        assertThat(restrictedPatient.supportingPrison).isEqualTo(PRISON)
        assertThat(restrictedPatient.hospitalLocation).isEqualTo(HOSPITAL)

        assertThat(restrictedPatient).extracting(
          "id",
          "prisonerNumber",
          "dischargeTime",
          "commentText",
          "createDateTime",
          "createUserId"
        ).contains(
          1L,
          "A12345",
          LocalDateTime.parse("2020-10-10T20:00:01"),
          "test",
          LocalDateTime.parse("2020-10-10T20:00:01"),
          "ITAG_USER"
        )
      }
    }
  }
}
