package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeLatestMovementReturn
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.Agency
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientEventService

class RestrictedPatientEventServiceTest {

  private val prisonApiApplicationGateway: PrisonApiGateway = mock()
  private val domainEventPublisher: DomainEventPublisher = mock()
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val service = RestrictedPatientEventService(
    prisonApiApplicationGateway,
    domainEventPublisher,
    restrictedPatientsRepository,
  )

  @Nested
  inner class PrisonerReleased {
    @BeforeEach
    fun beforeEach() {
      whenever(restrictedPatientsRepository.existsById(ArgumentMatchers.anyString())).thenReturn(false)
      whenever(prisonApiApplicationGateway.getLatestMovements(any())).thenReturn(
        listOf(
          makeLatestMovementReturn(
            movementType = "REL",
            movementReasonCode = "HP",
            toAgency = "HAZLWD",
          ),
        ),
      )
      whenever(prisonApiApplicationGateway.getAgency(any())).thenReturn(
        Agency(agencyId = "HAZLWD", description = "Hazelwood Hospital", agencyType = "HSHOSP"),
      )
      whenever(restrictedPatientsRepository.save(any())).thenReturn(makeRestrictedPatient())
    }

    @Nested
    inner class Failures {

      @Test
      fun `should do nothing if prisoner is already a restricted patient`() {
        whenever(restrictedPatientsRepository.existsById(ArgumentMatchers.anyString())).thenReturn(true)

        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository).existsById("A1234AA")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should throw when get movements fails with error`() {
        whenever(prisonApiApplicationGateway.getLatestMovements(any())).thenThrow(WebClientResponseException.BadGateway::class.java)

        Assertions.assertThrows(WebClientResponseException.BadGateway::class.java) {
          service.prisonerReleased("A1234AA")
        }

        verify(prisonApiApplicationGateway).getLatestMovements("A1234AA")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if movements not found`() {
        whenever(prisonApiApplicationGateway.getLatestMovements(any())).thenReturn(emptyList())

        service.prisonerReleased("A1234AA")

        verify(prisonApiApplicationGateway).getLatestMovements("A1234AA")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if latest movement not release to hospital`() {
        whenever(prisonApiApplicationGateway.getLatestMovements(any())).thenReturn(
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
        whenever(prisonApiApplicationGateway.getAgency(any())).thenThrow(WebClientResponseException.BadRequest::class.java)

        Assertions.assertThrows(WebClientResponseException.BadRequest::class.java) {
          service.prisonerReleased("A1234AA")
        }

        verify(prisonApiApplicationGateway).getAgency("HAZLWD")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if to agency is not found`() {
        whenever(prisonApiApplicationGateway.getAgency(any())).thenReturn(null)

        service.prisonerReleased("A1234AA")

        verify(prisonApiApplicationGateway).getAgency("HAZLWD")
        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }

      @Test
      fun `should do nothing if agency is not a hospital`() {
        whenever(prisonApiApplicationGateway.getLatestMovements(any())).thenReturn(
          listOf(
            makeLatestMovementReturn(
              movementType = "REL",
              movementReasonCode = "HP",
              toAgency = "MDI",
            ),
          ),
        )
        whenever(prisonApiApplicationGateway.getAgency(any())).thenReturn(
          Agency(agencyId = "MDI", description = "MDI", agencyType = "INST"),
        )

        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository, never()).save(any())
        verify(domainEventPublisher, never()).publishRestrictedPatientAdded(any())
      }
    }

    @Nested
    inner class Success {

      @AfterEach
      fun afterEach() {
        verify(prisonApiApplicationGateway).getLatestMovements("A1234AA")
        verify(prisonApiApplicationGateway).getAgency("HAZLWD")
        verify(restrictedPatientsRepository).existsById("A1234AA")
      }

      @Test
      fun `should create restricted patient`() {
        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository).save(
          org.mockito.kotlin.check {
            with(it) {
              org.assertj.core.api.Assertions.assertThat(prisonerNumber).isEqualTo("A1234AA")
              org.assertj.core.api.Assertions.assertThat(fromLocationId).isEqualTo("MDI")
              org.assertj.core.api.Assertions.assertThat(hospitalLocationCode).isEqualTo("HAZLWD")
              org.assertj.core.api.Assertions.assertThat(supportingPrisonId).isEqualTo("MDI")
              org.assertj.core.api.Assertions.assertThat(dischargeTime)
                .isEqualTo(java.time.LocalDateTime.parse("2022-05-01T15:33:11"))
              org.assertj.core.api.Assertions.assertThat(commentText).isEqualTo("Released for some reason")
            }
          },
        )
      }

      @Test
      fun `should also allow release to agency type HOSPITAL`() {
        whenever(prisonApiApplicationGateway.getAgency(any())).thenReturn(
          Agency(agencyId = "HAZLWD", description = "Hazelwood Hospital", agencyType = "HOSPITAL"),
        )

        service.prisonerReleased("A1234AA")

        verify(restrictedPatientsRepository).save(
          org.mockito.kotlin.check {
            org.assertj.core.api.Assertions.assertThat(it.hospitalLocationCode).isEqualTo("HAZLWD")
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
