package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.OffenderBookingResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

class BreakFlow : RuntimeException()

@ActiveProfiles("test", "app-scope")
@Tag("race-condition-test")
class RestrictedPatientServiceIntegrationTest : IntegrationTestBase() {

  @MockBean
  lateinit var prisonApiGateway: PrisonApiGateway

  @Autowired
  lateinit var restrictedPatientsService: RestrictedPatientsService

  @Autowired
  lateinit var restrictedPatientsRepository: RestrictedPatientsRepository

  @Nested
  inner class DischargeToHospital {

    @BeforeEach
    fun beforeEach() {
      whenever(prisonApiGateway.getOffenderBooking("F12345")).thenReturn(OffenderBookingResponse(1235467, "F12345", true))
    }

    @Test
    fun `ensure that the restricted patient is fully committed to the db before making the call to prison api`() {
      doAnswer {
        getRestrictedPatient(prisonerNumber = "F12345").exchange().expectStatus().is2xxSuccessful
        throw BreakFlow()
      }.whenever(prisonApiGateway).dischargeToHospital(any(), anyBoolean())

      Assertions.assertThrows(BreakFlow::class.java) {
        restrictedPatientsService.dischargeToHospital(makeDischargeRequest().copy(offenderNo = "F12345"))
      }
    }

    @Test
    fun `the recently added restricted patient gets removed`() {
      doAnswer { throw BreakFlow() }.whenever(prisonApiGateway).dischargeToHospital(any(), anyBoolean())

      Assertions.assertThrows(BreakFlow::class.java) {
        restrictedPatientsService.dischargeToHospital(makeDischargeRequest().copy(offenderNo = "F12345"))
      }

      getRestrictedPatient(prisonerNumber = "F12345").exchange().expectStatus().isNotFound
    }
  }

  @Nested
  inner class RemoveRestrictedPatient {

    @BeforeEach
    fun beforeEach() {
      prisonerSearchApiMockServer.stubSearchByPrisonNumber("Z123456")
      restrictedPatientsRepository.saveAndFlush(makeRestrictedPatient(prisonerNumber = "Z123456"))
    }

    @Test
    fun `ensure that the restricted patient has been removed before creating an external movement`() {
      doAnswer {
        getRestrictedPatient(prisonerNumber = "Z123456").exchange().expectStatus().isNotFound
        throw BreakFlow()
      }.whenever(prisonApiGateway).createExternalMovement(any())

      Assertions.assertThrows(BreakFlow::class.java) {
        restrictedPatientsService.removeRestrictedPatient(prisonerNumber = "Z123456")
      }
    }

    @Test
    fun `the recently deleted restricted patient gets re added`() {
      doAnswer { throw BreakFlow() }.whenever(prisonApiGateway).createExternalMovement(any())

      Assertions.assertThrows(BreakFlow::class.java) {
        restrictedPatientsService.removeRestrictedPatient(prisonerNumber = "Z123456")
      }

      getRestrictedPatient(prisonerNumber = "Z123456").exchange().expectStatus().is2xxSuccessful
    }
  }
}
