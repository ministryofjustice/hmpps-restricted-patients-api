package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Component
class PrisonApiHealthCheck @Autowired constructor(
  prisonApiNoAuthToken: WebClient,
  @Value("\${api.health-timeout-ms}") timeout: Duration
) : HealthCheck(prisonApiNoAuthToken, timeout)
