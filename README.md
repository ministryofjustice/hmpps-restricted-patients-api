# HMPPS Restricted Patients API
[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/hmpps-restricted-patients-api/badge?style=flat)](https://github-community.service.justice.gov.uk/repository-standards/hmpps-restricted-patients-api)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-restricted-patients-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://restricted-patients-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/hmpps-restricted-patients-api/main/async-api.yml&readOnly)

Restricted Patients keeps track of prisoners who have been moved to a hospital but are still the
responsibility of a prison. This API holds the list of current restricted patients, writes the hospital
movements to NOMIS through Prison API, and publishes domain events so prisoner search and other
services stay up to date.

The front end is [hmpps-restricted-patients-ui](https://github.com/ministryofjustice/hmpps-restricted-patients-ui).

**Developer documentation, for both the API and the UI, is in [docs/](docs/README.md)**: what the
service does, how it fits together, deployment and a runbook.

# Features
* Discharge a prisoner to hospital
* Add a prisoner already released to hospital in NOMIS
* Change a restricted patient's supporting prison
* Remove restricted patients, by hand, when received back into prison, or nightly once custody has ended
* Pick up discharges to hospital made directly in NOMIS

# Instructions
## Tests
Before running the tests:
 - `docker compose -f docker-compose-test.yml up` needs to be running and to have finished loading
before you start running the tests. Once done you can run the tests by running `./gradlew build`.

## Running locally
`./gradlew bootRun --args='--spring.profiles.active=dev,stdout,localstack'`

This needs the test containers above, a dev HMPPS Auth client and access to the dev Prison API and
Prisoner Search. See [docs/getting-started.md](docs/getting-started.md).

## HMPPS domain events
This service publishes `restricted-patients.patient.added`, `restricted-patients.patient.removed` and
`restricted-patients.patient.supporting-prison-changed`, and consumes `OFFENDER_MOVEMENT-RECEPTION`,
`prisoner-offender-search.prisoner.released` and `prison-offender-events.prisoner.merged`. See
[docs/architecture.md](docs/architecture.md#events) and the event docs linked above.

Merged prisoner records are detected but not handled automatically; see
[docs/runbook.md](docs/runbook.md#a-restricted-patient-has-been-merged).
