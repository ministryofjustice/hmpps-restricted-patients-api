package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class UserSecurityDetailsTest {

  @Test
  fun `username as the principal`() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")

    assertThat(UserSecurityUtils().currentUsername).isEqualTo("user")
  }

  @Test
  fun `user details as the principal`() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(TestUserDetails("user"), "pw")

    assertThat(UserSecurityUtils().currentUsername).isEqualTo("user")
  }

  @Test
  fun `map as the principal`() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(mapOf("username" to "user"), "pw")

    assertThat(UserSecurityUtils().currentUsername).isEqualTo("user")
  }

  @Test
  fun `principal is null`() {
    SecurityContextHolder.getContext().authentication = null

    assertThat(UserSecurityUtils().currentUsername).isEqualTo(null)
  }
}
