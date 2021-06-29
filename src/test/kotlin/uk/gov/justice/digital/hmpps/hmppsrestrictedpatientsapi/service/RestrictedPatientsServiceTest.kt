package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeToHospitalResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService
import java.time.LocalDateTime

class RestrictedPatientsServiceTest {

  private val prisonApiGateway: PrisonApiGateway = mock()

  @Test
  fun `call discharge offender using the correct parameters`() {

    whenever(prisonApiGateway.dischargeToHospital(any())).thenReturn(makeDischargeToHospitalResponse())

    val service = RestrictedPatientsService(prisonApiGateway)
    val response = service.dischargeToHospital(makeDischargeRequest())

    assertThat(response).isEqualTo(makeDischargeToHospitalResponse())

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
  }
}