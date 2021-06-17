package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsRestrictedPatientsApi

fun main(args: Array<String>) {
  runApplication<HmppsRestrictedPatientsApi>(*args)
}
