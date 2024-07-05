package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import org.springframework.data.domain.AuditorAware
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.util.Optional

class AuditorAware(private val authenticationFacade: HmppsAuthenticationHolder) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> = Optional.ofNullable(
    // use the nullable version here since for the event listener there is no principal in context
    authenticationFacade.authenticationOrNull?.principal,
  )
}
