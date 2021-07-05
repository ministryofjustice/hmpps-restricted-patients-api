package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.enums

enum class LegalStatus(private val desc: String) {
  RECALL("Recall"),
  DEAD("Dead"),
  INDETERMINATE_SENTENCE("Indeterminate Sentence"),
  SENTENCED("Sentenced"),
  CONVICTED_UNSENTENCED(
    "Convicted Unsentenced"
  ),
  CIVIL_PRISONER("Civil Prisoner"),
  IMMIGRATION_DETAINEE("Immigration Detainee"),
  REMAND("Remand"),
  UNKNOWN("Unknown"),
  OTHER(
    "Other"
  );

  companion object {
    fun permissibleLegalStatusesForDischargingPrisonersToHospital(): List<LegalStatus> = listOf(
      INDETERMINATE_SENTENCE,
      RECALL,
      SENTENCED,
      CONVICTED_UNSENTENCED,
      IMMIGRATION_DETAINEE
    )
  }
}
