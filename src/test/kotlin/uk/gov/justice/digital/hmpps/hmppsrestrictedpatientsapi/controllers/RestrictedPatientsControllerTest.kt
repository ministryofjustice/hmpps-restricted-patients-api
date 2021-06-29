package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService

@WebMvcTest(value = [RestrictedPatentsController::class])
class RestrictedPatientsControllerTest : ControllerTestBase() {

  @MockBean
  lateinit var restrictedPatientsService: RestrictedPatientsService

  @Test
  @WithMockUser(username = "ITAG_USER")
  fun `call the service with the correct parameters`() {
    mockMvc
      .perform(
        MockMvcRequestBuilders.post("/discharge-to-hospital")
          .header("Content-Type", "application/json")
          .content(
            objectMapper.writeValueAsString(
              makeDischargeBody()
            )
          )
      )
      .andExpect(MockMvcResultMatchers.status().isCreated)

    verify(restrictedPatientsService).dischargeToHospital(makeDischargeRequest())
  }

  @Test
  @WithMockUser(username = "ITAG_USER")
  fun `handle internal exceptions`() {
    doThrow(RuntimeException()).`when`(restrictedPatientsService).dischargeToHospital(any())

    mockMvc
      .perform(
        MockMvcRequestBuilders.post("/discharge-to-hospital")
          .header("Content-Type", "application/json")
          .content(
            objectMapper.writeValueAsString(
              makeDischargeBody()
            )
          )
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
              )
            )
          )
      )
      .andExpect(MockMvcResultMatchers.status().isBadRequest)
  }

  private fun makeDischargeBody() = mapOf(
    "offenderNo" to "A12345",
    "commentText" to "test",
    "dischargeTime" to "2020-10-10T20:00:01",
    "fromLocationId" to "MDI",
    "hospitalLocationCode" to "HAZLWD",
    "supportingPrisonId" to "MDI"
  )
}
