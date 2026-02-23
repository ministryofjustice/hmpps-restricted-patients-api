package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.BadRequestException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.HOSPITAL
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.PRISON
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeLatestMovementReturn
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeMigrateInRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeSupportingPrisonRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiQueryService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiUpdateService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class RestrictedPatientServiceTest {

  private val prisonApiUpdateService: PrisonApiUpdateService = mock()
  private val prisonApiQueryService: PrisonApiQueryService = mock()
  private val prisonerSearchApiService: PrisonerSearchApiService = mock()
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val clock: Clock = mock()

  private val restrictedPatient = RestrictedPatient(
    prisonerNumber = "A12345",
    fromLocationId = "MDI",
    hospitalLocationCode = "HAZLWD",
    supportingPrisonId = "LEI",
    dischargeTime = LocalDateTime.now(),
    commentText = "test",
  )

  private val service = RestrictedPatientsService(
    prisonApiQueryService,
    prisonApiUpdateService,
    prisonerSearchApiService,
    restrictedPatientsRepository,
    clock,
  )

  private val argumentCaptor = argumentCaptor<RestrictedPatient>()

  @BeforeEach
  fun beforeEach() {
    val fixedClock =
      Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
    whenever(clock.instant()).thenReturn(fixedClock.instant())
    whenever(clock.zone).thenReturn(fixedClock.zone)
  }

  @Nested
  inner class RemovesRestrictedPatient {
    @Test
    fun `checks to see if there is a restricted patient associated with the prisoner number`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )
      whenever(prisonerSearchApiService.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      verify(restrictedPatientsRepository).findById("A12345")
    }

    @Test
    fun `handles no restricted patient record exists`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.empty())

      val exception = assertThrows(EntityNotFoundException::class.java) {
        service.removeRestrictedPatient("A12345")
      }

      assertThat(exception.message).isEqualTo("No restricted patient record found for prison number A12345")
    }

    @Test
    fun `makes a call to prisoner offender search`() {
      whenever(prisonerSearchApiService.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )

      service.removeRestrictedPatient("A12345")

      verify(prisonerSearchApiService).searchByPrisonNumber("A12345")
    }

    @Test
    fun `handles no prisoner offender search results returned`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )
      whenever(prisonerSearchApiService.searchByPrisonNumber(anyString())).thenReturn(emptyList())

      val exception = assertThrows(EntityNotFoundException::class.java) {
        service.removeRestrictedPatient("A12345")
      }

      assertThat(exception.message).isEqualTo("No prisoner search results returned for A12345")
    }

    @Test
    fun `makes a call to prison api to create an external movement form hospital to community`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )
      whenever(prisonerSearchApiService.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      verify(prisonApiUpdateService).createExternalMovement(
        CreateExternalMovement(
          bookingId = 1L,
          fromAgencyId = "HAZLWD",
          toAgencyId = "OUT",
          movementTime = LocalDateTime.now(clock),
          movementType = "REL",
          movementReason = "CR",
          directionCode = "OUT",
        ),
      )
    }

    @Test
    fun `remove restricted patient`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.of(makeRestrictedPatient()))
      whenever(prisonerSearchApiService.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      verify(restrictedPatientsRepository).delete(argumentCaptor.capture())

      assertThat(argumentCaptor.firstValue).extracting(
        "prisonerNumber",
        "fromLocationId",
        "hospitalLocationCode",
        "supportingPrisonId",
        "dischargeTime",
        "commentText",
      ).contains("A12345", "MDI", "HAZLWD", "MDI", LocalDateTime.parse("2020-10-10T20:00:01"), "test")
    }

    @Test
    fun `ensures that the restricted patient is removed before the prison api calls`() {
      whenever(restrictedPatientsRepository.findById(any())).thenReturn(Optional.of(makeRestrictedPatient()))
      whenever(prisonerSearchApiService.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      val inOrder = inOrder(restrictedPatientsRepository, prisonApiUpdateService)

      inOrder.verify(restrictedPatientsRepository).delete(any())
      inOrder.verify(prisonApiUpdateService).createExternalMovement(any())
    }
  }

  @Nested
  inner class DischargeToHospital {
    @Nested
    inner class Failures {
      @Test
      fun `throws exception when offender not found`() {
        whenever(prisonApiQueryService.getOffenderBooking(any())).thenReturn(null)

        assertThrows(EntityNotFoundException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }
      }

      @Test
      fun `throws exception when offender is OUT`() {
        whenever(prisonApiQueryService.getOffenderBooking(any())).thenReturn(
          OffenderBookingResponse(1234567, "A1234AA", false),
        )

        assertThrows(EntityNotFoundException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }
      }

      @Test
      fun `throws exception when the offender is already a restricted patient`() {
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.of(makeRestrictedPatient()))

        assertThrows(BadRequestException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }
      }
    }

    @Nested
    inner class SuccessfulDischargeToHospital {
      @BeforeEach
      fun beforeEach() {
        whenever(prisonApiQueryService.getOffenderBooking(any())).thenReturn(
          OffenderBookingResponse(1234567, "A1234AA", true),
        )
        whenever(restrictedPatientsRepository.save(any<RestrictedPatient>())).thenReturn(makeRestrictedPatient())
      }

      @Test
      fun `make a call to prison api to discharge a prisoner to hospital`() {
        val patient = makeRestrictedPatient(supportingPrisonId = "LEI")
        whenever(restrictedPatientsRepository.save(any<RestrictedPatient>())).thenReturn(patient)
        val response = service.dischargeToHospital(makeDischargeRequest().copy(supportingPrisonId = "LEI"))

        verify(prisonApiUpdateService).dischargeToHospital(patient)

        assertThat(response.fromLocation).isEqualTo(Agency(agencyId = "MDI"))
        assertThat(response.supportingPrison).isEqualTo(Agency(agencyId = "LEI"))
        assertThat(response.hospitalLocation).isEqualTo(Agency(agencyId = "HAZLWD"))

        assertThat(response)
          .extracting(
            "prisonerNumber",
            "dischargeTime",
            "commentText",
          )
          .contains("A12345", LocalDateTime.parse("2020-10-10T20:00:01"), "test")
      }

      @Test
      fun `default to from prison id for supporting prison when not supplied`() {
        service.dischargeToHospital(makeDischargeRequest())

        verify(prisonApiUpdateService).dischargeToHospital(restrictedPatient)
      }

      @Test
      fun `calls save with the correct parameters`() {
        val now = LocalDate.now().atStartOfDay()
        service.dischargeToHospital(makeDischargeRequest())

        verify(restrictedPatientsRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.firstValue).extracting(
          "fromLocationId",
          "supportingPrisonId",
          "hospitalLocationCode",
          "commentText",
          "dischargeTime",
        ).contains("MDI", "MDI", "HAZLWD", "test", now)
      }

      @Test
      fun `ensures that the restricted patient is saved to the database before the prison api call`() {
        service.dischargeToHospital(makeDischargeRequest())

        val inOrder = inOrder(restrictedPatientsRepository, prisonApiUpdateService)

        inOrder.verify(restrictedPatientsRepository).save(any())
        inOrder.verify(prisonApiUpdateService).dischargeToHospital(any())
      }
    }

    @Nested
    inner class GetRestrictedPatient {
      @Test
      fun `throws entity not found`() {
        whenever(restrictedPatientsRepository.findById(any())).thenReturn(Optional.empty())

        assertThrows(EntityNotFoundException::class.java) {
          service.getRestrictedPatient("A12345")
        }
      }

      @Test
      fun `by prison number`() {
        whenever(prisonApiQueryService.getAgencyLocationsByType("HSHOSP")).thenReturn(listOf(HOSPITAL))
        whenever(prisonApiQueryService.getAgencyLocationsByType("INST")).thenReturn(listOf(PRISON))
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
          Optional.of(makeRestrictedPatient()),
        )

        val restrictedPatient = service.getRestrictedPatient("A12345")

        assertThat(restrictedPatient.fromLocation).isEqualTo(PRISON)
        assertThat(restrictedPatient.supportingPrison).isEqualTo(PRISON)
        assertThat(restrictedPatient.hospitalLocation).isEqualTo(HOSPITAL)

        assertThat(restrictedPatient).extracting(
          "prisonerNumber",
          "dischargeTime",
          "commentText",
          "createDateTime",
          "createUserId",
        ).contains(
          "A12345",
          LocalDateTime.parse("2020-10-10T20:00:01"),
          "test",
          LocalDateTime.parse("2020-10-10T20:00:01"),
          "ITAG_USER",
        )
      }
    }
  }

  @Nested
  inner class MigrateInPatient {
    @Nested
    inner class Failures {
      @Test
      fun `throws exception when the offender is already a restricted patient`() {
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.of(makeRestrictedPatient()))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender has not got any previous movements`() {
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(emptyList())

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when Prison API returns multiple movements`() {
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(),
            makeLatestMovementReturn(),
          ),
        )

        assertThrows(RuntimeException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement is not a REL`() {
        val nonRelMovement = makeLatestMovementReturn().copy(
          movementType = "TPT",
        )
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(listOf(nonRelMovement))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement does not have a from agency`() {
        val movementWithoutFromAgency = makeLatestMovementReturn().copy(
          fromAgency = null,
        )
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(listOf(movementWithoutFromAgency))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement has an invalid date`() {
        val movementWithInvalidDate = makeLatestMovementReturn().copy(
          movementDate = null,
        )
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(listOf(movementWithInvalidDate))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement has an invalid time`() {
        val movementWithInvalidTime = makeLatestMovementReturn().copy(
          movementTime = null,
        )
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(listOf(movementWithInvalidTime))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }
    }

    @Nested
    inner class SuccessfulMigration {
      private val dischargeDate = "2022-05-02"
      private val dischargeTime = "15:00:11"
      private val dischargeDateTime = LocalDateTime.parse("${dischargeDate}T$dischargeTime")
      private val testComment = "A test comment"

      private val migratedRestrictedPatient = RestrictedPatient(
        prisonerNumber = "A12345",
        fromLocationId = "MDI",
        hospitalLocationCode = "HAZLWD",
        supportingPrisonId = "MDI",
        dischargeTime = dischargeDateTime,
        commentText = testComment,
      )

      @BeforeEach
      fun beforeEach() {
        whenever(restrictedPatientsRepository.save(any<RestrictedPatient>())).thenReturn(
          makeRestrictedPatient(
            dischargeTime = dischargeDateTime,
            commentText = testComment,
          ),
        )
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementDate = dischargeDate,
              movementTime = dischargeTime,
              commentText = testComment,
            ),
          ),
        )
      }

      @Test
      fun `it makes a call to prison api to update discharge record`() {
        val response = service.migrateInPatient(makeMigrateInRequest())

        verify(prisonApiUpdateService).dischargeToHospital(migratedRestrictedPatient)

        assertThat(response.fromLocation).isEqualTo(Agency(agencyId = "MDI"))
        assertThat(response.supportingPrison).isEqualTo(Agency(agencyId = "MDI"))
        assertThat(response.hospitalLocation).isEqualTo(Agency(agencyId = "HAZLWD"))

        assertThat(response)
          .extracting(
            "prisonerNumber",
            "dischargeTime",
            "commentText",
          )
          .contains("A12345", dischargeDateTime, testComment)
      }

      @Test
      fun `calls save with the correct parameters`() {
        service.migrateInPatient(makeMigrateInRequest())

        verify(restrictedPatientsRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.firstValue.fromLocationId).isEqualTo("MDI")
        assertThat(argumentCaptor.firstValue.supportingPrisonId).isEqualTo("MDI")
        assertThat(argumentCaptor.firstValue.hospitalLocationCode).isEqualTo("HAZLWD")
        assertThat(argumentCaptor.firstValue.commentText).isEqualTo("Historical discharge to hospital added to restricted patients")
        assertThat(argumentCaptor.firstValue.dischargeTime).isEqualTo(LocalDateTime.parse("${dischargeDate}T$dischargeTime"))
      }

      @Test
      fun `saves restricted patient data before the index is updated`() {
        service.migrateInPatient(makeMigrateInRequest())
        verify(restrictedPatientsRepository).save(any())
      }
    }

    @Nested
    inner class SuccessfulMigrationWithCommentsSanitised {
      private val dischargeDate = "2022-05-02"
      private val dischargeTime = "15:00:11"
      private val dischargeDateTime = LocalDateTime.parse("${dischargeDate}T$dischargeTime")
      private val testComment = "A test comment with TO=ST NICH''S NEWCASTLE"

      @BeforeEach
      fun beforeEach() {
        whenever(restrictedPatientsRepository.save(any<RestrictedPatient>())).thenReturn(
          makeRestrictedPatient(
            dischargeTime = dischargeDateTime,
            commentText = "comment saved to restricted patients",
          ),
        )
        whenever(prisonApiQueryService.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementDate = dischargeDate,
              movementTime = dischargeTime,
              commentText = testComment,
            ),
          ),
        )
      }

      @Test
      fun `it makes a call to prison api to update discharge record`() {
        val response = service.migrateInPatient(makeMigrateInRequest())
        assertThat(response.commentText).isEqualTo("comment saved to restricted patients")
        verify(restrictedPatientsRepository).save(
          check {
            assertThat(it.commentText).isEqualTo("Historical discharge to hospital added to restricted patients")
          },
        )
        verify(prisonApiUpdateService).dischargeToHospital(
          newRestrictedPatient = check {
            assertThat(it.commentText).isEqualTo("comment saved to restricted patients")
          },
        )
      }
    }
  }

  @Nested
  inner class ChangeSupportingPrison {
    @Nested
    inner class Failures {
      @Test
      fun `throws exception when the offender is not found`() {
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.empty())

        assertThatThrownBy {
          service.changeSupportingPrison(makeSupportingPrisonRequest())
        }.isInstanceOf(EntityNotFoundException::class.java)
      }

      @Test
      fun `throws exception when the offender is already supported by MDI`() {
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
          Optional.of(
            makeRestrictedPatient(
              supportingPrisonId = "MDI",
            ),
          ),
        )

        assertThatThrownBy {
          service.changeSupportingPrison(makeSupportingPrisonRequest(supportingPrisonId = "MDI"))
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessageContaining("already supported by MDI")
      }

      @Test
      fun `throws exception when the supporting prison is not found`() {
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
          Optional.of(
            makeRestrictedPatient(
              supportingPrisonId = "MDI",
            ),
          ),
        )

        assertThatThrownBy {
          service.changeSupportingPrison(makeSupportingPrisonRequest(supportingPrisonId = "MDI"))
        }.isInstanceOf(BadRequestException::class.java)
          .hasMessageContaining("already supported by MDI")
      }
    }

    @Nested
    inner class SuccessfulChange {
      @Test
      fun `returns supporting prison`() {
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
          Optional.of(makeRestrictedPatient(supportingPrisonId = "LEI")),
        )
        whenever(prisonApiQueryService.getAgency(anyString())).thenReturn(PRISON)
        whenever(restrictedPatientsRepository.save(any<RestrictedPatient>())).thenReturn(makeRestrictedPatient(supportingPrisonId = "MDI"))
        whenever(prisonApiQueryService.getAgencyLocationsByType("HSHOSP")).thenReturn(listOf(HOSPITAL))
        whenever(prisonApiQueryService.getAgencyLocationsByType("INST")).thenReturn(listOf(PRISON))

        val response = service.changeSupportingPrison(makeSupportingPrisonRequest(supportingPrisonId = "MDI"))

        assertThat(response.supportingPrison?.agencyId).isEqualTo("MDI")
      }

      @Test
      fun `calls save`() {
        whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
          Optional.of(makeRestrictedPatient()),
        )
        whenever(restrictedPatientsRepository.save(any<RestrictedPatient>())).thenReturn(makeRestrictedPatient(supportingPrisonId = "LEI"))
        whenever(prisonApiQueryService.getAgency(anyString())).thenReturn(PRISON)

        service.changeSupportingPrison(makeSupportingPrisonRequest(supportingPrisonId = "LEI"))

        verify(restrictedPatientsRepository).findById("A12345")
        verify(restrictedPatientsRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.firstValue.supportingPrisonId).isEqualTo("LEI")
      }
    }
  }
}
