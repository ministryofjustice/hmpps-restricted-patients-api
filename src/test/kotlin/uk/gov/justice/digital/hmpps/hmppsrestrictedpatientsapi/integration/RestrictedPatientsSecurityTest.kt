package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import java.util.stream.Stream

@DisplayName("Restricted patient endpoint security")
class RestrictedPatientsSecurityTest : IntegrationTestBase() {

  @ParameterizedTest(name = "{0} {1} needs a token")
  @MethodSource("allEndpoints")
  fun `access unauthorised when no token`(method: HttpMethod, uri: String) {
    webTestClient.method(method).uri(uri)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest(name = "{0} {1} forbidden without a role")
  @MethodSource("writeEndpoints")
  fun `access forbidden when no role`(method: HttpMethod, uri: String, role: String, body: Map<String, String>) {
    webTestClient.method(method).uri(uri)
      .headers(setHeaders(roles = listOf()))
      .bodyValue(body)
      .exchange()
      .expectStatus().isForbidden
  }

  @ParameterizedTest(name = "{0} {1} forbidden with the wrong role")
  @MethodSource("writeEndpoints")
  fun `access forbidden with wrong role`(method: HttpMethod, uri: String, role: String, body: Map<String, String>) {
    val otherRoles = WRITE_ROLES - role
    webTestClient.method(method).uri(uri)
      .headers(setHeaders(roles = otherRoles.toList()))
      .bodyValue(body)
      .exchange()
      .expectStatus().isForbidden
  }

  @ParameterizedTest(name = "{0} {1} allowed with {2}")
  @MethodSource("writeEndpoints")
  fun `access allowed with the role`(method: HttpMethod, uri: String, role: String, body: Map<String, String>) {
    // Nothing is stubbed, so the request fails further in, but it gets past the role check
    webTestClient.method(method).uri(uri)
      .headers(setHeaders(roles = listOf(role)))
      .bodyValue(body)
      .exchange()
      .expectStatus().value { assert(it != 401 && it != 403) { "Expected to get past the role check, got $it" } }
  }

  @Test
  fun `get forbidden when no role`() {
    webTestClient.get().uri(PATIENT_URI)
      .headers(setHeaders(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get forbidden with the other write roles`() {
    webTestClient.get().uri(PATIENT_URI)
      .headers(setHeaders(roles = listOf("ROLE_TRANSFER_RESTRICTED_PATIENT", "ROLE_RESTRICTED_PATIENT_MIGRATION")))
      .exchange()
      .expectStatus().isForbidden
  }

  @ParameterizedTest(name = "get allowed with {0}")
  @ValueSource(strings = ["ROLE_RESTRICTED_PATIENTS__RO", "ROLE_REMOVE_RESTRICTED_PATIENT"])
  fun `get allowed with a read role`(role: String) {
    webTestClient.get().uri(PATIENT_URI)
      .headers(setHeaders(roles = listOf(role)))
      .exchange()
      .expectStatus().isNotFound
  }

  companion object {
    private const val PATIENT_URI = "/restricted-patient/prison-number/A12345"

    private val WRITE_ROLES = setOf(
      "ROLE_TRANSFER_RESTRICTED_PATIENT",
      "ROLE_RESTRICTED_PATIENT_MIGRATION",
      "ROLE_REMOVE_RESTRICTED_PATIENT",
    )

    @JvmStatic
    fun writeEndpoints(): Stream<Arguments> = Stream.of(
      Arguments.of(
        HttpMethod.POST,
        "/discharge-to-hospital",
        "ROLE_TRANSFER_RESTRICTED_PATIENT",
        mapOf("offenderNo" to "A12345", "fromLocationId" to "MDI", "hospitalLocationCode" to "HAZLWD", "supportingPrisonId" to "MDI"),
      ),
      Arguments.of(
        HttpMethod.POST,
        "/migrate-in-restricted-patient",
        "ROLE_RESTRICTED_PATIENT_MIGRATION",
        mapOf("offenderNo" to "A12345", "hospitalLocationCode" to "HAZLWD"),
      ),
      Arguments.of(
        HttpMethod.POST,
        "/change-supporting-prison",
        "ROLE_RESTRICTED_PATIENT_MIGRATION",
        mapOf("offenderNo" to "A12345", "supportingPrisonId" to "MDI"),
      ),
      Arguments.of(HttpMethod.DELETE, PATIENT_URI, "ROLE_REMOVE_RESTRICTED_PATIENT", mapOf<String, String>()),
    )

    @JvmStatic
    fun allEndpoints(): Stream<Arguments> = Stream.concat(
      writeEndpoints().map { Arguments.of(it.get()[0], it.get()[1]) },
      Stream.of(Arguments.of(HttpMethod.GET, PATIENT_URI)),
    )
  }
}
