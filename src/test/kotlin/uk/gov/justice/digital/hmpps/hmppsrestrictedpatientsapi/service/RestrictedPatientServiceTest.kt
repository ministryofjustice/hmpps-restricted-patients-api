package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config.BadRequestException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.HOSPITAL
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.PRISON
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeLatestMovementReturn
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeMigrateInRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makePrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.CreateExternalMovement
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class RestrictedPatientServiceTest {

  private val prisonApiGateway: PrisonApiGateway = mock()
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway = mock()
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val domainEventPublisher: DomainEventPublisher = mock()
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
    prisonApiGateway,
    prisonerSearchApiGateway,
    restrictedPatientsRepository,
    domainEventPublisher,
    clock,
  )

  @BeforeEach
  fun beforeEach() {
    val fixedClock =
      Clock.fixed(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
    whenever(clock.instant()).thenReturn(fixedClock.instant())
    whenever(clock.getZone()).thenReturn(fixedClock.getZone())
  }

  @Nested
  inner class RemovesRestrictedPatient {
    @Test
    fun `checks to see if there is a restricted patient associated with the prisoner number`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

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
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )

      service.removeRestrictedPatient("A12345")

      verify(prisonerSearchApiGateway).searchByPrisonNumber("A12345")
    }

    @Test
    fun `handles no prisoner offender search results returned`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(emptyList())

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
          directionCode = "OUT",
        ),
      )
    }

    @Test
    fun `remove restricted patient`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(Optional.of(makeRestrictedPatient()))
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

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
    fun `ensures that the restricted patient is removed before the prison api calls`() {
      whenever(restrictedPatientsRepository.findById(any())).thenReturn(Optional.of(makeRestrictedPatient()))
      whenever(prisonerSearchApiGateway.searchByPrisonNumber(anyString())).thenReturn(listOf(makePrisonerResult()))

      service.removeRestrictedPatient("A12345")

      val inOrder = inOrder(restrictedPatientsRepository, prisonApiGateway)

      inOrder.verify(restrictedPatientsRepository).delete(any())
      inOrder.verify(prisonApiGateway).createExternalMovement(any())
    }
  }

  @Nested
  inner class DischargeToHospital {
    @Nested
    inner class Failures {
      @Test
      fun `throws exception when offender not found`() {
        whenever(prisonApiGateway.getOffenderBooking(any())).thenReturn(null)

        assertThrows(EntityNotFoundException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }
      }

      @Test
      fun `throws exception when offender is OUT`() {
        whenever(prisonApiGateway.getOffenderBooking(any())).thenReturn(
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
        whenever(prisonApiGateway.getOffenderBooking(any())).thenReturn(
          OffenderBookingResponse(1234567, "A1234AA", true),
        )
        whenever(restrictedPatientsRepository.save(any())).thenReturn(makeRestrictedPatient())
      }

      @Test
      fun `make a call to prison api to discharge a prisoner to hospital`() {
        val patient = makeRestrictedPatient(supportingPrisonId = "LEI")
        whenever(restrictedPatientsRepository.save(any())).thenReturn(patient)
        val response = service.dischargeToHospital(makeDischargeRequest().copy(supportingPrisonId = "LEI"))

        verify(prisonApiGateway).dischargeToHospital(patient)

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

        verify(prisonApiGateway).dischargeToHospital(restrictedPatient)
      }

      @Test
      fun `calls save with the correct parameters`() {
        val argumentCaptor = ArgumentCaptor.forClass(RestrictedPatient::class.java)
        val now = LocalDate.now().atStartOfDay()
        service.dischargeToHospital(makeDischargeRequest())

        verify(restrictedPatientsRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.value).extracting(
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

        val inOrder = inOrder(restrictedPatientsRepository, prisonApiGateway)

        inOrder.verify(restrictedPatientsRepository).save(any())
        inOrder.verify(prisonApiGateway).dischargeToHospital(any())
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
        whenever(prisonApiGateway.getAgencyLocationsByType("HSHOSP")).thenReturn(listOf(HOSPITAL))
        whenever(prisonApiGateway.getAgencyLocationsByType("INST")).thenReturn(listOf(PRISON))
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
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(emptyList())

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when Prison API returns multiple movements`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(listOf(makeLatestMovementReturn(), makeLatestMovementReturn()))

        assertThrows(RuntimeException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement is not a REL`() {
        val nonRelMovement = makeLatestMovementReturn().copy(
          movementType = "TPT",
        )
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(listOf(nonRelMovement))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement does not have a from agency`() {
        val movementWithoutFromAgency = makeLatestMovementReturn().copy(
          fromAgency = null,
        )
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(listOf(movementWithoutFromAgency))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement has an invalid date`() {
        val movementWithInvalidDate = makeLatestMovementReturn().copy(
          movementDate = null,
        )
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(listOf(movementWithInvalidDate))

        assertThrows(BadRequestException::class.java) {
          service.migrateInPatient(makeMigrateInRequest())
        }
      }

      @Test
      fun `throws exception when the offender's latest movement has an invalid time`() {
        val movementWithInvalidTime = makeLatestMovementReturn().copy(
          movementTime = null,
        )
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(listOf(movementWithInvalidTime))

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
        whenever(restrictedPatientsRepository.save(any())).thenReturn(
          makeRestrictedPatient(
            dischargeTime = dischargeDateTime,
            commentText = testComment,
          ),
        )
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
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

        verify(prisonApiGateway).dischargeToHospital(migratedRestrictedPatient)

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
        val argumentCaptor = ArgumentCaptor.forClass(RestrictedPatient::class.java)
        service.migrateInPatient(makeMigrateInRequest())

        verify(restrictedPatientsRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.value.fromLocationId).isEqualTo("MDI")
        assertThat(argumentCaptor.value.supportingPrisonId).isEqualTo("MDI")
        assertThat(argumentCaptor.value.hospitalLocationCode).isEqualTo("HAZLWD")
        assertThat(argumentCaptor.value.commentText).isEqualTo("Historical discharge to hospital added to restricted patients")
        assertThat(argumentCaptor.value.dischargeTime).isEqualTo(LocalDateTime.parse("${dischargeDate}T$dischargeTime"))
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
        whenever(restrictedPatientsRepository.save(any())).thenReturn(
          makeRestrictedPatient(
            dischargeTime = dischargeDateTime,
            commentText = "comment saved to restricted patients",
          ),
        )
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
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
        verify(prisonApiGateway).dischargeToHospital(
          newRestrictedPatient = check {
            assertThat(it.commentText).isEqualTo("comment saved to restricted patients")
          },
        )
      }
    }
  }

  @Nested
  inner class PrisonerReleased {
    @Nested
    inner class Failures {
      @Test
      fun `should throw when get movements fails with error`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenThrow(WebClientResponseException.BadGateway::class.java)

        assertThrows(WebClientResponseException.BadGateway::class.java) {
          service.prisonerReleased("A1234AA")
        }

        verify(prisonApiGateway).getLatestMovements("A1234AA")
      }

      @Test
      fun `should do nothing if movements not found`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(emptyList())

        service.prisonerReleased("A1234AA")

        verify(prisonApiGateway).getLatestMovements("A1234AA")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if latest movement not release to hospital`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementType = "REL",
              movementReasonCode = "CR",
              toAgency = "OUT",
            ),
          ),
        )

        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should throw if get agency location fails with error`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementType = "REL",
              movementReasonCode = "HP",
              toAgency = "UNKNOWN",
            ),
          ),
        )
        whenever(prisonApiGateway.getAgencyLocation(any())).thenThrow(WebClientResponseException.BadRequest::class.java)

        assertThrows(WebClientResponseException.BadRequest::class.java) {
          service.prisonerReleased("A1234AA")
        }

        verify(prisonApiGateway).getAgencyLocation("UNKNOWN")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if to agency is not found`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementType = "REL",
              movementReasonCode = "HP",
              toAgency = "UNKNOWN",
            ),
          ),
        )
        whenever(prisonApiGateway.getAgencyLocation(any())).thenReturn(null)

        service.prisonerReleased("A1234AA")

        verify(prisonApiGateway).getAgencyLocation("UNKNOWN")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if agency is not a hospital`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementType = "REL",
              movementReasonCode = "HP",
              toAgency = "MDI",
            ),
          ),
        )
        whenever(prisonApiGateway.getAgencyLocation(any())).thenReturn(
          Agency(agencyId = "MDI", description = "MDI", agencyType = "INST"),
        )

        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if prisoner is already a restricted patient`() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementType = "REL",
              movementReasonCode = "HP",
              toAgency = "HAZLWD",
            ),
          ),
        )
        whenever(prisonApiGateway.getAgencyLocation(any())).thenReturn(
          Agency(agencyId = "HAZLWD", description = "Hazelwood Hospital", agencyType = "HSHOSP"),
        )
        whenever(restrictedPatientsRepository.existsById(anyString())).thenReturn(true)

        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository).existsById("A1234AA")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }
    }

    @Nested
    inner class Success {

      @BeforeEach
      fun beforeEach() {
        whenever(prisonApiGateway.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementType = "REL",
              movementReasonCode = "HP",
              toAgency = "HAZLWD",
            ),
          ),
        )
        whenever(prisonApiGateway.getAgencyLocation(any())).thenReturn(
          Agency(agencyId = "HAZLWD", description = "Hazelwood Hospital", agencyType = "HSHOSP"),
        )
        whenever(restrictedPatientsRepository.existsById(anyString())).thenReturn(false)
        whenever(restrictedPatientsRepository.save(any())).thenReturn(makeRestrictedPatient())
      }

      @AfterEach
      fun afterEach() {
        verify(prisonApiGateway).getLatestMovements("A1234AA")
        verify(prisonApiGateway).getAgencyLocation("HAZLWD")
        verify(restrictedPatientsRepository).existsById("A1234AA")
      }

      @Test
      fun `should create restricted patient`() {
        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository).save(
          check {
            with(it) {
              assertThat(prisonerNumber).isEqualTo("A1234AA")
              assertThat(fromLocationId).isEqualTo("MDI")
              assertThat(hospitalLocationCode).isEqualTo("HAZLWD")
              assertThat(supportingPrisonId).isEqualTo("MDI")
              assertThat(dischargeTime).isEqualTo(LocalDateTime.parse("2022-05-01T15:33:11"))
              assertThat(commentText).isEqualTo("Released for some reason")
            }
          },
        )
      }

      @Test
      fun `should also allow release to agency type HOSPITAL`() {
        whenever(prisonApiGateway.getAgencyLocation(any())).thenReturn(
          Agency(agencyId = "HAZLWD", description = "Hazelwood Hospital", agencyType = "HOSPITAL"),
        )

        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository).save(
          check {
            assertThat(it.hospitalLocationCode).isEqualTo("HAZLWD")
          },
        )
      }

      @Test
      fun `should publish domain event`() {
        service.prisonerReleased("A1234AA")

        verify(domainEventPublisher).publishRestrictedPatientAdded("A1234AA")
      }
    }
  }
}
