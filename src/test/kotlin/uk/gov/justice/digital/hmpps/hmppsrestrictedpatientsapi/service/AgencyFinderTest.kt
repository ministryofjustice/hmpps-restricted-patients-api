package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.AgencyFinder

@ActiveProfiles("test")
class AgencyFinderTest : IntegrationTestBase() {

  @Autowired
  private lateinit var agencyFinder: AgencyFinder

  @Test
  fun `finds hospitals`() {
    assertAll(
      { assertThat(agencyFinder.findHospitalCode("UNKNOWN")).isNull() },
      { assertThat(agencyFinder.findHospitalCode("Harplands Hospital")).isEqualTo("HLHOSP") }
    )
  }

  @Test
  fun `finds prisons`() {
    assertAll(
      { assertThat(agencyFinder.findPrisonCode("UNKNOWN")).isNull() },
      { assertThat(agencyFinder.findPrisonCode("HMP Foston Hall")).isEqualTo("FHI") }
    )
  }
}
