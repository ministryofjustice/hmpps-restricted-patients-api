# Hmpps restricted patients API 
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-restricted-patients-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-restricted-patients-api)
[![Docker](https://quay.io/repository/hmpps/hmpps-restricted-patients-api/status)](https://quay.io/repository/hmpps-restricted-patients-api/status)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://restricted-patients-api-dev.hmpps.service.justice.gov.uk/swagger-ui/?configUrl=/v3/api-docs)

# Features
* Discharge  a prisoner to hospital 
* Surface a restricted patients details
* Remove restricted patients from the service 

The frontend can be found here: <https://github.com/ministryofjustice/hmpps-restricted-patients>

# Instructions
###Tests
Before running the tests:
 - `docker-compose -f docker-compose-test.yml up` needs to be running and to have finished loading 
before you start running the tests. Once done you can run the tests by running `./gradlew build`.
 - Start the localstack instance using the SQS library, e.g.
   - `cd ../hmpps-spring-boot-sqs`
   - `docker-compose -f docker-compose-test.yml up localstack`
   - See the instructions in the `hmpps-spring-boot-sqs` project for me details

###Running locally 
`./gradlew bootRun --args='--spring.profiles.active=dev,stdout,localstack'`

## Domain events
This service publishes the  `restricted-patients.patient.removed` domain event whenever a restricted patient 
is removed from the service. 

###Publish -> restricted-patients.patient.removed
The message is published via amazon sns. The payload is defined below. 
```javascript
{
   "eventType": "restricted-patients.patient.removed",
   "occurredAt": "2021-02-08T14:41:11.526762Z", //ISO offset date time when the restricted patient was removed
   "publishedAt": "2021-02-08T14:41:11.526762Z", //ISO offset date time when the event was published
   "version": 1, 
   "description": "Prisoner no longer a restricted patient"     
   "additionalInformation": { "prisonerNumber": "A12345"}     
}
```

This service subscribes to two types of events:
* prison-offender-events.prisoner.received
* restricted-patients.patient.removed 

Restricted patients are removed automatically when a `prison-offender-events.prisoner.received` domain event has been received.

When the service receives a `restricted-patients.patient.removed ` the event will logged and then ignored. This is for ease of testing.

