package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.dataBuilders.makeDischargeToHospitalResponse
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.gateways.PrisonerSearchApiGateway
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums.LegalStatus
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions.NoResultsReturnedException
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.request.DischargeToHospitalRequest
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.response.PrisonerResult
import uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services.RestrictedPatientsService
import java.time.LocalDateTime
import javax.validation.ValidationException

class RestrictedPatientsServiceTest {

  private val prisonApiGateway: PrisonApiGateway = mock()
  private val prisonerSearchApiGateway: PrisonerSearchApiGateway = mock()

  private lateinit var service: RestrictedPatientsService

  @BeforeEach
  fun beforeEach() {
    service = RestrictedPatientsService(prisonApiGateway, prisonerSearchApiGateway)
  }

  @Nested
  inner class DischargeToHospital {
    @Nested
    inner class Failures {

      @Test
      fun `will not discharge a prisoner with the wrong legal status`() {
        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          listOf(PrisonerResult(prisonerNumber = "A12345", legalStatus = LegalStatus.UNKNOWN))
        )
        Assertions.assertThrows(ValidationException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }

        verify(prisonApiGateway, never()).dischargeToHospital(any())
      }

      @Test
      fun `throws not found exception`() {
        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          emptyList()
        )

        Assertions.assertThrows(NoResultsReturnedException::class.java) {
          service.dischargeToHospital(makeDischargeRequest())
        }
      }
    }

    @Nested
    inner class SuccessfulDischargeToHospital {
      @ParameterizedTest
      @EnumSource(
        value = LegalStatus::class,
        names = ["INDETERMINATE_SENTENCE", "RECALL", "SENTENCED", "CONVICTED_UNSENTENCED", "IMMIGRATION_DETAINEE"]
      )
      fun `discharge an prisoner to hospital when they have the correct legal status`(status: LegalStatus) {
        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          listOf(PrisonerResult(prisonerNumber = "A12345", legalStatus = status))
        )

        service.dischargeToHospital(makeDischargeRequest())

        verify(prisonApiGateway).dischargeToHospital(any())
      }

      @Test
      fun `make a call to prison api to discharge a prisoner to hospital`() {

        whenever(prisonerSearchApiGateway.searchByPrisonNumber(any())).thenReturn(
          listOf(
            PrisonerResult(
              prisonerNumber = "A12345",
              legalStatus = LegalStatus.SENTENCED
            )

          )
        )
        whenever(prisonApiGateway.dischargeToHospital(any())).thenReturn(makeDischargeToHospitalResponse())

        val response = service.dischargeToHospital(makeDischargeRequest())

        assertThat(response).isEqualTo(makeDischargeToHospitalResponse())

        verify(prisonApiGateway).dischargeToHospital(
          DischargeToHospitalRequest(
            offenderNo = "A12345",
            commentText = "test",
            dischargeTime = LocalDateTime.parse("2020-10-10T20:00:01"),
            fromLocationId = "MDI",
            hospitalLocationCode = "HAZLWD",
            supportingPrisonId = "MDI"
          )
        )
      }
    }
  }
}
