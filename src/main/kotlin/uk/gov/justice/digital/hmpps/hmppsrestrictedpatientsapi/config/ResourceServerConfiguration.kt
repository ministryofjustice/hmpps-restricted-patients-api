package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration : WebSecurityConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http.headers().frameOptions().sameOrigin().and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      // Can't have CSRF protection as requires session
      .and().csrf().disable()
      .authorizeRequests { auth ->
        auth.antMatchers(
          "/webjars/**", "/favicon.ico", "/csrf",
          "/health/**", "/info",
          "/v3/api-docs/**", "/api/swagger.json", "/swagger-ui/**",
          "/v3/api-docs", "/swagger-ui.html",
          "/swagger-resources", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security",
          "/queue-admin/retry-all-dlqs",
        ).permitAll()
          .anyRequest()
          .authenticated()
      }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
  }
}
