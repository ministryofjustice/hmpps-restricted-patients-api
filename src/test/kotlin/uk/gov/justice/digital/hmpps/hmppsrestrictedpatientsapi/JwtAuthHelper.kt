package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi

import io.jsonwebtoken.Jwts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Configuration
class JwtAuthHelper {
  private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

  @Bean
  @Primary
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  fun createJwt(
    subject: String? = null,
    userId: String? = "${subject}_ID",
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    expiryTime: Duration = Duration.ofHours(1),
    clientId: String = "test-client-id",
    jwtId: String = UUID.randomUUID().toString(),
  ): String {
    val claims = mutableMapOf<String, Any?>("user_name" to subject, "client_id" to clientId, "user_id" to userId)
    roles?.let { claims["authorities"] = roles }
    scope?.let { claims["scope"] = scope }
    return Jwts.builder()
      .id(jwtId)
      .subject(subject)
      .claims(claims)
      .expiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(keyPair.private, Jwts.SIG.RS256)
      .compact()
  }
}
