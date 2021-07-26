package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient

interface RestrictedPatientsRepository : CrudRepository<RestrictedPatient, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): RestrictedPatient?
}
