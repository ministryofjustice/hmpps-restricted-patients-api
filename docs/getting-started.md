# Getting started

How to build, test and run the API and the UI on your machine, and the things that trip people up.

## API (this repository)

Needs Java 25 and Docker.

### Tests

```bash
docker compose -f docker-compose-test.yml up -d   # Postgres 18 on 5432, localstack (SNS, SQS) on 4566
./gradlew check
```

- The integration tests use a **real Postgres and localstack** from that compose file, not
  Testcontainers. Postgres must be on **port 5432**, so stop any other local Postgres (for
  example another service's test database) first.
- Prison API, Prisoner Search and HMPPS Auth are WireMock servers on ports 8989, 8100 and 8090.
  Port 8090 is also used by other HMPPS services' tests, so do not run two test suites at once.
- The clock is fixed at 2020-10-10 in tests, and the database is cleaned and migrated after each
  test.
- CI runs `./gradlew assemble check` (see `.github/workflows/pipeline.yml`).
- The tests tagged `race-condition-test` (`RestrictedPatientServiceIntegrationTest`) are left out
  of `test` and are not run in CI.

### Running locally

```bash
docker compose -f docker-compose-test.yml up -d
export RESTRICTED_PATIENTS_API_CLIENT_ID=...      # a dev client with access to Prison API and Prisoner Search
export RESTRICTED_PATIENTS_API_CLIENT_SECRET=...
./gradlew bootRun --args='--spring.profiles.active=dev,stdout,localstack'
```

The `dev` profile uses the local Postgres and the **real dev** Prison API, Prisoner Search and
HMPPS Auth (`application-dev.yml`), so you need network access to them and a client id and secret.
The service's own dev credentials are in the `hmpps-restricted-patients-api-client-creds` secret in
the `hmpps-restricted-patients-dev` namespace. Remember that writes go to dev NOMIS through
Prison API.

Swagger UI is at `http://localhost:8080/swagger-ui/index.html` with the `dev` profile.

`docker-compose.yml` (the app plus its dependencies) is out of date and is not the recommended way
to run the service.

### Running the nightly job locally

Start the app with `--batch.enabled=true --batch.type=PAST_DATE_CHECK`. It runs the job once and
exits, without listening to the queues.

## UI ([hmpps-restricted-patients-ui](https://github.com/ministryofjustice/hmpps-restricted-patients-ui))

Needs Node 24 and npm 12 (see `engines` in `package.json`) and Docker.

```bash
npm run setup          # npm ci
npm run build
npm run lint
npm run typecheck
npm run test           # Jest unit tests
```

### Integration tests (Playwright)

```bash
docker compose -f docker-compose-test.yml up -d   # WireMock on 9091
npm run build
npm run start-feature  # the app on port 3007 using feature.env, every API pointing at WireMock
npm run int-test-init:ci   # once, installs the Playwright browser
npm run int-test           # or int-test-ui for the Playwright UI
```

`start-feature` runs the built `dist/server.js`, so build first. Integration test users get all
four roles by default (`integration_tests/testUtils.ts`).

### Running against dev

`npm run start:dev` (esbuild in watch mode) reads a `.env` file (not committed). It needs the dev URLs from
`helm_deploy/values-dev.yaml`, the auth code and client credentials from the
`hmpps-restricted-patients-ui-auth-code` and `-client-creds` secrets in the dev namespace, and
`REDIS_ENABLED=false`. The README's `docker compose up` route does not currently work.

## Getting a dev user

To use the dev UI you need a DPS user with one or more of the four roles
(`TRANSFER_RESTRICTED_PATIENT`, `SEARCH_RESTRICTED_PATIENT`, `REMOVE_RESTRICTED_PATIENT`,
`RESTRICTED_PATIENT_MIGRATION`) and a caseload with prisoners in it. Moving a test prisoner to
hospital in dev writes a real dev NOMIS movement; use "Remove from restricted patients" or a NOMIS
reception to put them back.
