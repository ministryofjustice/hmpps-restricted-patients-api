package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

@WebMvcTest(
  RestrictedPatientsController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class RestrictedPatientControllerTest : ControllerTestBase() {
  @MockBean
  lateinit var restrictedPatientsService: RestrictedPatientsService

  @MockBean
  lateinit var domainEventPublisher: DomainEventPublisher

  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @Nested
  inner class DischargeToHospital {
    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `call the service with the correct parameters`() {
      whenever(restrictedPatientsService.dischargeToHospital(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/discharge-to-hospital") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeDischargeBody())
      }.andExpect { status { isCreated() } }

      verify(restrictedPatientsService).dischargeToHospital(makeDischargeRequest().copy(supportingPrisonId = "MDI"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `will publish add domain event`() {
      whenever(restrictedPatientsService.dischargeToHospital(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/discharge-to-hospital") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeDischargeBody())
      }.andExpect { status { isCreated() } }

      verify(domainEventPublisher).publishRestrictedPatientAdded("A12345")
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `will publish add telemetry event`() {
      whenever(restrictedPatientsService.dischargeToHospital(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/discharge-to-hospital") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeDischargeBody())
      }.andExpect { status { isCreated() } }

      verify(telemetryClient).trackEvent(
        "restricted-patient-added",
        mapOf(
          "prisonerNumber" to "A12345",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI",
          "dischargeTime" to "2020-10-10T20:00:01",
        ),
        null,
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `handle internal exceptions`() {
      doThrow(RuntimeException()).whenever(restrictedPatientsService).dischargeToHospital(any())

      mockMvc
        .perform(
          MockMvcRequestBuilders.post("/discharge-to-hospital")
            .header("Content-Type", "application/json")
            .content(
              objectMapper.writeValueAsString(
                makeDischargeBody(),
              ),
            ),
        )
        .andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `handle missing fields`() {
      mockMvc
        .perform(
          MockMvcRequestBuilders.post("/discharge-to-hospital")
            .header("Content-Type", "application/json")
            .content(
              objectMapper.writeValueAsString(
                mapOf(
                  "offenderNo" to "A12345",
                ),
              ),
            ),
        )
        .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `handle no results found exceptions`() {
      whenever(restrictedPatientsService.dischargeToHospital(any())).thenThrow(EntityNotFoundException::class.java)

      mockMvc
        .perform(
          MockMvcRequestBuilders.post("/discharge-to-hospital")
            .header("Content-Type", "application/json")
            .content(
              objectMapper.writeValueAsString(makeDischargeBody()),
            ),
        )
        .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
  }

  @Nested
  @WithMockUser(username = "ITAG_USER")
  inner class RetrieveRestrictedPatient {
    @Test
    fun `restricted patient not found by prison number`() {
      whenever(restrictedPatientsService.getRestrictedPatient("A12345")).thenThrow(EntityNotFoundException())

      mockMvc
        .perform(
          MockMvcRequestBuilders.get("/restricted-patient/prison-number/A12345")
            .header("Content-Type", "application/json")
            .content(
              objectMapper.writeValueAsString(makeRestrictedPatientDto()),
            ),
        )
        .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `returns a restricted patient for a prisoner number`() {
      whenever(restrictedPatientsService.getRestrictedPatient("A12345")).thenReturn(makeRestrictedPatientDto())

      mockMvc
        .perform(
          MockMvcRequestBuilders.get("/restricted-patient/prison-number/A12345")
            .header("Content-Type", "application/json")
            .content(
              objectMapper.writeValueAsString(makeRestrictedPatientDto()),
            ),
        )
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }
  }

  @Nested
  @WithMockUser(username = "ITAG_USER")
  inner class RemoveRestrictedPatients {
    @Test
    fun `remove restricted patient from the service`() {
      whenever(restrictedPatientsService.removeRestrictedPatient(any())).thenReturn(makeRestrictedPatient())
      mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/restricted-patient/prison-number/A12345"),
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

      verify(restrictedPatientsService).removeRestrictedPatient("A12345")
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `will publish remove domain event`() {
      whenever(restrictedPatientsService.removeRestrictedPatient(any())).thenReturn(makeRestrictedPatient())

      mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/restricted-patient/prison-number/A12345"),
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

      verify(domainEventPublisher).publishRestrictedPatientRemoved("A12345")
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `will publish add telemetry event`() {
      whenever(restrictedPatientsService.removeRestrictedPatient(any())).thenReturn(makeRestrictedPatient())

      mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/restricted-patient/prison-number/A12345"),
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

      verify(telemetryClient).trackEvent(
        "restricted-patient-removed",
        mapOf(
          "prisonerNumber" to "A12345",
          "fromLocationId" to "MDI",
          "hospitalLocationCode" to "HAZLWD",
          "supportingPrisonId" to "MDI",
          "dischargeTime" to "2020-10-10T20:00:01",
        ),
        null,
      )
    }
  }

  private fun makeDischargeBody() = mapOf(
    "offenderNo" to "A12345",
    "commentText" to "test",
    "fromLocationId" to "MDI",
    "hospitalLocationCode" to "HAZLWD",
    "supportingPrisonId" to "MDI",
  )
}
