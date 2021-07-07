package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

@RestController
class RestrictedPatentsController(private val restrictedPatientsService: RestrictedPatientsService) {

  @PostMapping(value = ["/discharge-to-hospital"])
  @ResponseStatus(code = HttpStatus.CREATED)
  fun dischargeToHospital(@RequestBody dischargeToHospital: DischargeToHospitalRequest): RestrictedPatientDto =
    restrictedPatientsService.dischargeToHospital(dischargeToHospital)


  @GetMapping(value = ["/restricted-patient/prison-number/{prison-number}"])
  fun getRestrictedPatientByPrisonNumber(@PathVariable(name = "prison-number") prisonNumber: String): RestrictedPatientDto =
    restrictedPatientsService.getRestrictedPatient(prisonNumber)
}
