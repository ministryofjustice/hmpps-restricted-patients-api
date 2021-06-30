package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.exceptions

class NoResultsReturnedException(override val message: String) : RuntimeException(message)
