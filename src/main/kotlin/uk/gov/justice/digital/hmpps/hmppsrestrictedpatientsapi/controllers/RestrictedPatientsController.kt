package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
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
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

@RestController
class RestrictedPatientsController(
  private val restrictedPatientsService: RestrictedPatientsService,
  private val domainEventPublisher: DomainEventPublisher,
  private val telemetryClient: TelemetryClient,
) {
  @PostMapping(
    value = ["/discharge-to-hospital"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseStatus(code = HttpStatus.CREATED)
  fun dischargeToHospital(@RequestBody dischargeToHospital: DischargeToHospitalRequest): RestrictedPatientDto =
    restrictedPatientsService.dischargeToHospital(dischargeToHospital).also {
      it.publishAndTrackAdd()
    }

  @PostMapping(
    value = ["/migrate-in-restricted-patient"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseStatus(code = HttpStatus.CREATED)
  fun migrateInPatient(@RequestBody migrateIn: MigrateInRequest): RestrictedPatientDto =
    restrictedPatientsService.migrateInPatient(migrateIn).also {
      it.publishAndTrackAdd()
    }

  private fun RestrictedPatientDto.publishAndTrackAdd() {
    domainEventPublisher.publishRestrictedPatientAdded(prisonerNumber)
    telemetryClient.trackEvent(
      "restricted-patient-added",
      mapOf(
        "prisonerNumber" to prisonerNumber,
        "fromLocationId" to fromLocation?.agencyId,
        "hospitalLocationCode" to hospitalLocation?.agencyId,
        "supportingPrisonId" to supportingPrison?.agencyId,
        "dischargeTime" to dischargeTime.toString(),
      ),
      null,
    )
  }

  @GetMapping(value = ["/restricted-patient/prison-number/{prison-number}"])
  fun getRestrictedPatientByPrisonNumber(@PathVariable(name = "prison-number") prisonNumber: String): RestrictedPatientDto =
    restrictedPatientsService.getRestrictedPatient(prisonNumber)

  @DeleteMapping(value = ["/restricted-patient/prison-number/{prison-number}"])
  fun removeRestrictedPatient(@PathVariable(name = "prison-number") prisonNumber: String) =
    restrictedPatientsService.removeRestrictedPatient(prisonNumber).also {
      domainEventPublisher.publishRestrictedPatientRemoved(it.prisonerNumber)
      telemetryClient.trackEvent(
        "restricted-patient-removed",
        mapOf(
          "prisonerNumber" to it.prisonerNumber,
          "fromLocationId" to it.fromLocationId,
          "hospitalLocationCode" to it.hospitalLocationCode,
          "supportingPrisonId" to it.supportingPrisonId,
          "dischargeTime" to it.dischargeTime.toString(),
        ),
        null,
      )
    }
}
