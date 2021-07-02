package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import org.springframework.data.domain.AuditorAware
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.security.AuthenticationFacade
import java.util.Optional

class AuditorAware(private val authenticationFacade: AuthenticationFacade) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> = Optional.ofNullable(authenticationFacade.currentUsername)
}
