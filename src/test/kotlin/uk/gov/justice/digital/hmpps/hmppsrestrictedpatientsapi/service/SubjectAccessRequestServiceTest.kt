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
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.repositories.RestrictedPatientsRepository
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.SubjectAccessRequestService
import java.time.LocalDateTime
import java.util.Optional

class SubjectAccessRequestServiceTest {
  private val restrictedPatientsRepository: RestrictedPatientsRepository = mock()
  private val service = SubjectAccessRequestService(restrictedPatientsRepository)

  @Nested
  inner class GetRestrictedPatient {
    @Test
    fun `throws entity not found`() {
      whenever(restrictedPatientsRepository.findById(any())).thenReturn(Optional.empty())

      assertThat(service.getContentFor("A12345", null, null)).isNull()
    }

    @Test
    fun `by prison number`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )

      val restrictedPatient = service.getContentFor("A12345", null, null)?.content

      assertThat(restrictedPatient?.prisonerNumber).isEqualTo("A12345")
      assertThat(restrictedPatient?.supportingPrisonId).isEqualTo(PRISON.agencyId)
      assertThat(restrictedPatient?.hospitalLocationCode).isEqualTo(HOSPITAL.agencyId)
      assertThat(restrictedPatient?.commentText).isEqualTo("test")
      assertThat(restrictedPatient?.dischargeTime).isEqualTo(LocalDateTime.parse("2020-10-10T20:00:01"))
    }
  }
}
