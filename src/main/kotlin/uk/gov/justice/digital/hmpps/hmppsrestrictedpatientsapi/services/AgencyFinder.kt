package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.services

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class AgencyFinder {

  private val allPrisons = loadResource("prison-codes.csv")
  private val allHospitals = loadResource("hospital-codes.csv")

  private fun loadResource(resource: String) =
    ClassPathResource("agencyFinder/$resource").inputStream
      .let { BufferedReader(InputStreamReader(it)) }
      .readLines()
      .map { it.split(",") }
      .associate { Pair(it[1], it[0]) }

  fun findPrisonCode(name: String) = allPrisons[name]

  fun findHospitalCode(name: String) = allHospitals[name]
}
