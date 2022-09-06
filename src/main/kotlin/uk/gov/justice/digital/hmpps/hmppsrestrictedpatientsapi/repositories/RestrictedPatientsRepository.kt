package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient

@Repository
interface RestrictedPatientsRepository : JpaRepository<RestrictedPatient, String>
