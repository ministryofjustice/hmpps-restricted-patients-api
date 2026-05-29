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
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.UserDetail
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
      assertThat(restrictedPatient).extracting("fromLocationName").isEqualTo(PRISON.agencyId)
      assertThat(restrictedPatient).extracting("createdDate").isEqualTo(LocalDateTime.parse("2020-10-10T20:00:01"))
      assertThat(restrictedPatient).extracting("createdUserSurname").isNull()
      assertThat(restrictedPatient).extracting("modifiedDate").isNull()
      assertThat(restrictedPatient).extracting("modifiedUserSurname").isNull()
    }

    @Test
    fun `by prison number with agency information`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(makeRestrictedPatient()),
      )
      whenever(prisonApiQueryService.getAgency(HOSPITAL.agencyId)).thenReturn(HOSPITAL)
      whenever(prisonApiQueryService.getAgency(PRISON.agencyId)).thenReturn(PRISON)

      val restrictedPatient = service.getPrisonContentFor("A12345", null, null)?.content

      assertThat(restrictedPatient).extracting("prisonerNumber").isEqualTo("A12345")
      assertThat(restrictedPatient).extracting("supportingPrisonId").isEqualTo(PRISON.agencyId)
      assertThat(restrictedPatient).extracting("hospitalLocationCode").isEqualTo(HOSPITAL.agencyId)
      assertThat(restrictedPatient).extracting("hospitalLocationDescription").isEqualTo(HOSPITAL.description)
      assertThat(restrictedPatient).extracting("commentText").isEqualTo("test")
      assertThat(restrictedPatient).extracting("dischargeTime").isEqualTo(LocalDateTime.parse("2020-10-10T20:00:01"))
      assertThat(restrictedPatient).extracting("fromLocationName").isEqualTo(PRISON.description)
    }

    @Test
    fun `populates created and modified surname from prison api user lookup`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(
          makeRestrictedPatient(
            createUserId = "CREATOR",
            modifyDateTime = LocalDateTime.parse("2021-05-05T10:11:12"),
            modifyUserId = "EDITOR",
          ),
        ),
      )
      whenever(prisonApiQueryService.getUser("CREATOR")).thenReturn(UserDetail("CREATOR", "Alice", "Adams"))
      whenever(prisonApiQueryService.getUser("EDITOR")).thenReturn(UserDetail("EDITOR", "Bob", "Brown"))

      val restrictedPatient = service.getPrisonContentFor("A12345", null, null)?.content

      assertThat(restrictedPatient).extracting("createdUserSurname").isEqualTo("Adams")
      assertThat(restrictedPatient).extracting("modifiedDate").isEqualTo(LocalDateTime.parse("2021-05-05T10:11:12"))
      assertThat(restrictedPatient).extracting("modifiedUserSurname").isEqualTo("Brown")
    }

    @Test
    fun `leaves surnames null when prison api user lookup returns nothing`() {
      whenever(restrictedPatientsRepository.findById(anyString())).thenReturn(
        Optional.of(
          makeRestrictedPatient(
            createUserId = "UNKNOWN",
            modifyDateTime = LocalDateTime.parse("2021-05-05T10:11:12"),
            modifyUserId = "UNKNOWN_TOO",
          ),
        ),
      )
      whenever(prisonApiQueryService.getUser("UNKNOWN")).thenReturn(null)
      whenever(prisonApiQueryService.getUser("UNKNOWN_TOO")).thenReturn(null)

      val restrictedPatient = service.getPrisonContentFor("A12345", null, null)?.content

      assertThat(restrictedPatient).extracting("createdUserSurname").isNull()
      assertThat(restrictedPatient).extracting("modifiedUserSurname").isNull()
    }
  }
}
