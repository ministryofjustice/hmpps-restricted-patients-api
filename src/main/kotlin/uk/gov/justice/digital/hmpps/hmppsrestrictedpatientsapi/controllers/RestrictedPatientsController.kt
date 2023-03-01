package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.MigrateInRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.RestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

@RestController
class RestrictedPatientsController(private val restrictedPatientsService: RestrictedPatientsService) {
  @PostMapping(
    value = ["/discharge-to-hospital"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseStatus(code = HttpStatus.CREATED)
  fun dischargeToHospital(@RequestBody dischargeToHospital: DischargeToHospitalRequest): RestrictedPatientDto =
    restrictedPatientsService.dischargeToHospital(dischargeToHospital)

  @PostMapping(
    value = ["/migrate-in-restricted-patient"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseStatus(code = HttpStatus.CREATED)
  fun migrateInPatient(@RequestBody migrateIn: MigrateInRequest): RestrictedPatientDto =
    restrictedPatientsService.migrateInPatient(migrateIn)

  @GetMapping(
    value = ["/restricted-patient/prison-number/{prison-number}"],
  )
  fun getRestrictedPatientByPrisonNumber(@PathVariable(name = "prison-number") prisonNumber: String): RestrictedPatientDto =
    restrictedPatientsService.getRestrictedPatient(prisonNumber)

  @DeleteMapping(value = ["/restricted-patient/prison-number/{prison-number}"])
  fun removeRestrictedPatient(@PathVariable(name = "prison-number") prisonNumber: String) =
    restrictedPatientsService.removeRestrictedPatient(prisonNumber)
}
