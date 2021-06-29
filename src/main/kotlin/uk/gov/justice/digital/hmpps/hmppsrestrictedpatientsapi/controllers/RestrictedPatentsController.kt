package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.DischargeToHospitalResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

@RestController
class RestrictedPatentsController(private val restrictedPatientsService: RestrictedPatientsService) {

  @PostMapping(value = ["/discharge-to-hospital"])
  @ResponseStatus(code = HttpStatus.CREATED)
  fun dischargeToHospital(@RequestBody dischargeToHospital: DischargeToHospitalRequest): DischargeToHospitalResponse =
    restrictedPatientsService.dischargeToHospital(dischargeToHospital)
}
