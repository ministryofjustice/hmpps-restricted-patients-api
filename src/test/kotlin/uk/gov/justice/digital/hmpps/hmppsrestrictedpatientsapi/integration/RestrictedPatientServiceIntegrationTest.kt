package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

@WithMockUser("ITAG_USER")
class RestrictedPatientServiceIntegrationTest : IntegrationTestBase() {

  @MockBean
  @Qualifier("PrisonApiGatewayWithAuth")
  lateinit var prisonApiGateway: PrisonApiGateway

  @Autowired
  lateinit var restrictedPatientsService: RestrictedPatientsService

  @Test
  fun `ensure that the restricted patient is fully committed to the db before making the call to prison api`() {
    prisonerSearchApiMockServer.stubSearchByPrisonNumber("F12345")

    doAnswer {
      getRestrictedPatient(prisonerNumber = "F12345").exchange().expectStatus().is2xxSuccessful
      throw RuntimeException()
    }.whenever(prisonApiGateway).dischargeToHospital(any())

    Assertions.assertThrows(RuntimeException::class.java) {
      restrictedPatientsService.dischargeToHospital(makeDischargeRequest().copy(offenderNo = "F12345"))
    }
  }
}
