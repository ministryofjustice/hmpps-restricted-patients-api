package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictivePatient

interface RestrictedPatientsRepository : CrudRepository<RestrictivePatient, Long>
