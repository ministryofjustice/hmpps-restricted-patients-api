package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ActiveProfiles("test")
@Import(JwtAuthorisationHelper::class)
open class ControllerTestBase {
  @Autowired
  internal lateinit var mockMvc: MockMvc

  @Autowired
  internal lateinit var objectMapper: ObjectMapper
}
