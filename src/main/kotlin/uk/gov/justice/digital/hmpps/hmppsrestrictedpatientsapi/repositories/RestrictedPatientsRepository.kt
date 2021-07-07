package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatients

interface RestrictedPatientsRepository : CrudRepository<RestrictedPatients, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): RestrictedPatients?
}
