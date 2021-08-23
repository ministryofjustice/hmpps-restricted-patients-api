package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities.RestrictedPatient

interface RestrictedPatientsRepository : JpaRepository<RestrictedPatient, String>
