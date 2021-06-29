package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.DischargeToHospitalResponse

@Service
class RestrictedPatientsService(private val prisonApiGateway: PrisonApiGateway) {
  fun dischargeToHospital(dischargeToHospital: DischargeToHospitalRequest): DischargeToHospitalResponse {
    return prisonApiGateway.dischargeToHospital(dischargeToHospital)
  }
}
