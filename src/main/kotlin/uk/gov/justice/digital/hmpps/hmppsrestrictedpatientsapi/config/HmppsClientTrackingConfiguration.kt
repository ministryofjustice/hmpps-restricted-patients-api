package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import com.nimbusds.jwt.SignedJWT
import io.opentelemetry.api.trace.Span
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.clienttracking.HmppsClientTrackingInterceptor
import java.text.ParseException

@Configuration
class HmppsClientTrackingConfiguration {
  @Bean
  fun hmppsClientTrackingInterceptor() = HmppsClientTrackingInterceptor { token ->
    if (token.startsWith("Bearer ") == true) {
      try {
        val jwtBody = SignedJWT.parse(token.replace("Bearer ", "")).jwtClaimsSet
        val user = jwtBody.getClaim("user_name")?.toString()
        val currentSpan = Span.current()
        user?.run {
          currentSpan.setAttribute("username", this) // username in customDimensions
          currentSpan.setAttribute("enduser.id", this) // user_Id at the top level of the request
        }
        currentSpan.setAttribute("clientId", jwtBody.getClaim("client_id").toString())
      } catch (e: ParseException) {
        log.warn("problem decoding jwt public key for application insights", e)
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
