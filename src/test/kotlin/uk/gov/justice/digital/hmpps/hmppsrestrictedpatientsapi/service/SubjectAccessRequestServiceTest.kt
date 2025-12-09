package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.HOSPITAL
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.PRISON
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiQueryService
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.SubjectAccessRequestService
import java.time.LocalDateTime
import java.util.Optional

class SubjectAccessRequestServiceTest {
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val prisonApiQueryService: PrisonApiQueryService = mock()
  private val service = SubjectAccessRequestService(restrictedPatientsRepository, prisonApiQueryService)

  @Nested
  inner class GetRestrictedPatient {
    @Test
    fun `throws entity not found`() {
      whenever(restrictedPatientsRepository.findById(any())).thenReturn(Optional.empty())

      assertThat(service.getPrisonContentFor("A12345", null, null)).isNull()
    }

    @Test
    fun `by prison number with no agency information`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )

      val restrictedPatient = service.getPrisonContentFor("A12345", null, null)?.content

      assertThat(restrictedPatient).extracting("prisonerNumber").isEqualTo("A12345")
      assertThat(restrictedPatient).extracting("supportingPrisonId").isEqualTo(PRISON.agencyId)
      assertThat(restrictedPatient).extracting("hospitalLocationCode").isEqualTo(HOSPITAL.agencyId)
      assertThat(restrictedPatient).extracting("hospitalLocationDescription").isEqualTo(HOSPITAL.agencyId)
      assertThat(restrictedPatient).extracting("commentText").isEqualTo("test")
      assertThat(restrictedPatient).extracting("dischargeTime").isEqualTo(LocalDateTime.parse("2020-10-10T20:00:01"))
    }

    @Test
    fun `by prison number with agency information`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )
      whenever(prisonApiQueryService.getAgency(HOSPITAL.agencyId)).thenReturn(HOSPITAL)

      val restrictedPatient = service.getPrisonContentFor("A12345", null, null)?.content

      assertThat(restrictedPatient).extracting("prisonerNumber").isEqualTo("A12345")
      assertThat(restrictedPatient).extracting("supportingPrisonId").isEqualTo(PRISON.agencyId)
      assertThat(restrictedPatient).extracting("hospitalLocationCode").isEqualTo(HOSPITAL.agencyId)
      assertThat(restrictedPatient).extracting("hospitalLocationDescription").isEqualTo(HOSPITAL.description)
      assertThat(restrictedPatient).extracting("commentText").isEqualTo("test")
      assertThat(restrictedPatient).extracting("dischargeTime").isEqualTo(LocalDateTime.parse("2020-10-10T20:00:01"))
    }
  }
}
