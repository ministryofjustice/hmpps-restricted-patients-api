package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.security

interface AuthenticationFacade {
  val currentUsername: String?
}
