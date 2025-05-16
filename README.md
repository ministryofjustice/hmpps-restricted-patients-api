# HMPPS Restricted Patients API
[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-restricted-patients-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-restricted-patients-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-restricted-patients-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://restricted-patients-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/hmpps-restricted-patients-api/main/async-api.yml&readOnly)

# Features
* Discharge a prisoner to hospital 
* Surface a restricted patients details
* Remove restricted patients from the service 

The frontend can be found here: <https://github.com/ministryofjustice/hmpps-restricted-patients>

# Instructions
## Tests
Before running the tests:
 - `docker-compose -f docker-compose-test.yml up` needs to be running and to have finished loading 
before you start running the tests. Once done you can run the tests by running `./gradlew build`.

## Running locally 
`./gradlew bootRun --args='--spring.profiles.active=dev,stdout,localstack'`

## HMPPS domain events
This service publishes two events- a `restricted-patients.patient.removed` domain event whenever a restricted patient 
is removed from the service and a `restricted-patients.patient.added` domain event whenever they are added.

### Publish -> restricted-patients.patient.added
The message is published via amazon sns. The payload is defined below. 
```json
{
   "eventType": "restricted-patients.patient.added",
   "occurredAt": "2021-02-08T14:41:11.526762Z", // ISO offset date time when the restricted patient was added
   "publishedAt": "2021-02-08T14:41:11.526762Z", // ISO offset date time when the event was published
   "version": 1, 
   "description": "Prisoner is now a restricted patient",   
   "additionalInformation": { "prisonerNumber": "A12345"}     
}
```
### Publish -> restricted-patients.patient.removed
The message is published via amazon sns. The payload is defined below. 
```json
{
   "eventType": "restricted-patients.patient.removed",
   "occurredAt": "2021-02-08T14:41:11.526762Z", // ISO offset date time when the restricted patient was removed
   "publishedAt": "2021-02-08T14:41:11.526762Z", // ISO offset date time when the event was published
   "version": 1, 
   "description": "Prisoner no longer a restricted patient",    
   "additionalInformation": { "prisonerNumber": "A12345"}     
}
```

This service subscribes to a `prison-offender-events.prisoner.received`, which indicates that a prisoner has been
received into prison.  We check to see if the prisoner was a restricted patient and remove them if necessary.

### Subscribe -> prison-offender-events.prisoner.merged
We listen into prisoner merge events to see if a restricted patient has been merged.  At present the code is detection
only, so we spot a merge and then throw an exception so that the message is kept on the dead letter queue and constantly
retried.  This is because merges don't happen that frequently anyway and the code to deal with the merge would be
quite complicated, so it is better for now to just require manual intervention to resolve.  A restricted patient is
also still serving an active sentence so wouldn't normally be expected to be merged with a different record.

When a message is stuck on the dead letter queue look in application insights
```
traces
| where cloud_RoleName == 'hmpps-restricted-patients-api'
```
should show the issue.  There will be a message similar to
```
Merge not implemented. Patient A1234BC was at hospital ESURRY but record merged into B2345CD
```
If the merged offender is now in prison then it could just be a case of manually removing the database
record from restricted patients. This could be either of the prisoner numbers depending on which was the oldest NOMS
number.
If the merged offender is outside of prison then the movements and sentence information need to be investigated to work
out whether the prisoner should still be in hospital or has now been released.  If the prisoner has been released then
again the database record needs to be removed.  If the prisoner should still be in hospital then it could be a case of
changing the database record to point to the new prisoner number, or removing one of the rows if the prisoner was in
hospital under both records.

It has been known for the merge to be done the wrong way round with an old booking now as the latest booking record,
rather than the movement to hospital being the last booking record.  In that case raise the issue in #ask-prison-nomis,
it could be the case that syscon have to run a script to amend the sequence of the bookings in the merge.

**Note that after changing any database records the prisoner will have to be re-indexed in search.**

## Prisoner offender events
### Subscribe -> OFFENDER_MOVEMENT-RECEPTION

This service subscribes to a `OFFENDER_MOVEMENT-RECEPTION`, which indicates that a prisoner has been
received into prison.  We check to see if the prisoner was a restricted patient and remove them if necessary.
