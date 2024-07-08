package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder

@Configuration
@EnableJpaAuditing
class AuditConfiguration {
  @Bean
  fun auditorAware(authenticationFacade: HmppsAuthenticationHolder): AuditorAware<String> = AuditorAware(authenticationFacade)
}
