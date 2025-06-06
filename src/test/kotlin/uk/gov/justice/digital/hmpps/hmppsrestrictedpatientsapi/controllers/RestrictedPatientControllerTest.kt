package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatientDto
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeSupportingPrisonRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.DomainEventPublisher
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@WebMvcTest(
  RestrictedPatientsController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ClientWebSecurityAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class RestrictedPatientControllerTest : ControllerTestBase() {
  @MockitoBean
  lateinit var restrictedPatientsService: RestrictedPatientsService

  @MockitoBean
  lateinit var domainEventPublisher: DomainEventPublisher

  @MockitoBean
  lateinit var telemetryClient: TelemetryClient

  @Nested
  inner class DischargeToHospital {
    @Test
    @WithMockAuthUser(username = "ITAG_USER")
    fun `call the service with the correct parameters`() {
      whenever(restrictedPatientsService.dischargeToHospital(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/discharge-to-hospital") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeDischargeBody())
      }.andExpect { status { isCreated() } }

      verify(restrictedPatientsService).dischargeToHospital(makeDischargeRequest().copy(supportingPrisonId = "MDI"))
    }

    @Test
    @WithMockAuthUser(username = "ITAG_USER")
    fun `will publish add domain event`() {
      whenever(restrictedPatientsService.dischargeToHospital(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/discharge-to-hospital") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeDischargeBody())
      }.andExpect { status { isCreated() } }

      verify(domainEventPublisher).publishRestrictedPatientAdded("A12345")
    }

    @Test
    @WithMockAuthUser(username = "ITAG_USER")
    fun `will publish add telemetry event`() {
      whenever(restrictedPatientsService.dischargeToHospital(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/discharge-to-hospital") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeDischargeBody())
      }.andExpect { status { isCreated() } }

      verify(telemetryClient).trackEvent(
        "restricted-patient-added-discharge",
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
    @WithMockAuthUser(username = "ITAG_USER")
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
    @WithMockAuthUser(username = "ITAG_USER")
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
    @WithMockAuthUser(username = "ITAG_USER")
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
  @WithMockAuthUser(username = "ITAG_USER")
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
  @WithMockAuthUser(username = "ITAG_USER")
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
    @WithMockAuthUser(username = "ITAG_USER")
    fun `will publish remove domain event`() {
      whenever(restrictedPatientsService.removeRestrictedPatient(any())).thenReturn(makeRestrictedPatient())

      mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/restricted-patient/prison-number/A12345"),
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

      verify(domainEventPublisher).publishRestrictedPatientRemoved("A12345")
    }

    @Test
    @WithMockAuthUser(username = "ITAG_USER")
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

  @Nested
  @WithMockAuthUser(username = "ITAG_USER")
  inner class ChangeSupportingPrison {
    @Test
    fun `change a prisoner supporting prison`() {
      whenever(restrictedPatientsService.changeSupportingPrison(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/change-supporting-prison") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeSupportingPrisonRequest())
      }.andExpect { status { isOk() } }

      verify(restrictedPatientsService).changeSupportingPrison(
        org.mockito.kotlin.check {
          assertThat(it.supportingPrisonId).isEqualTo("MDI")
          assertThat(it.offenderNo).isEqualTo("A12345")
        },
      )
    }

    @Test
    fun `will publish change domain event`() {
      whenever(restrictedPatientsService.changeSupportingPrison(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/change-supporting-prison") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeSupportingPrisonRequest())
      }.andExpect { status { isOk() } }

      verify(domainEventPublisher).publishSupportingPrisonChanged("A12345")
    }

    @Test
    fun `will publish add telemetry event`() {
      whenever(restrictedPatientsService.changeSupportingPrison(any())).thenReturn(makeRestrictedPatientDto())

      mockMvc.post("/change-supporting-prison") {
        header("Content-Type", "application/json")
        content = objectMapper.writeValueAsString(makeSupportingPrisonRequest())
      }.andExpect { status { isOk() } }

      verify(telemetryClient).trackEvent(
        "restricted-patient-changed-supporting-prison",
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
