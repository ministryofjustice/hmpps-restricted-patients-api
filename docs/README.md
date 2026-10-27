# Restricted Patients: developer documentation

Restricted Patients is the DPS service prison staff use to keep track of **prisoners who have been
moved to a hospital**, usually a secure hospital under the Mental Health Act, but who are still the
responsibility of a prison. In NOMIS these people have been released, so without this service they
would disappear from view. Here they keep a **supporting prison**, so that prison's staff can still
find them, open their profile and add case notes, and they are removed automatically when their
custody ends or they come back to prison.

The service has two parts: a staff-facing UI
([hmpps-restricted-patients-ui](https://github.com/ministryofjustice/hmpps-restricted-patients-ui))
and this API. These docs cover both.

The things to know up front:

- **NOMIS is still the system of record for the movement.** Moving someone to hospital, or removing
  them, writes a movement to NOMIS through Prison API. This service holds only the list of current
  restricted patients, one row per prisoner, deleted when they stop being one.
- **Prisoner search is how everyone else sees the data.** The API publishes an event on every
  change; prisoner search re-indexes the prisoner and the UI's searches read from that index. A change
  made directly in the database is invisible until the prisoner is re-indexed.
- **Several things happen without anyone using the UI**: a discharge recorded directly in NOMIS is
  picked up automatically, a reception back into prison removes the patient, and a nightly job
  removes anyone whose custody has ended.
- **Merged NOMIS records are not handled automatically.** They land on a dead letter queue and raise
  an alert for someone to sort out by hand.

## Where to start

Read **business flows → architecture → runbook**. The rest are reference.

| Document | Read this when... |
| --- | --- |
| [business-flows.md](business-flows.md) | You want to know what the service does, journey by journey, including the automatic paths. |
| [architecture.md](architecture.md) | You need the components, the data, the endpoints, the events in and out, and who depends on us. |
| [runbook.md](runbook.md) | An alert has fired, a user cannot find someone, or a record needs fixing by hand. |
| [deployment.md](deployment.md) | You are releasing, or looking for a namespace, a queue, a secret or where alerts go. |
| [getting-started.md](getting-started.md) | You want the API or the UI running locally and the tests passing. |
| [history.md](history.md) | A ticket key or a design choice in the code needs explaining. |

## Repository layout (API)

| Path | What lives there |
| --- | --- |
| `src/main/kotlin/.../controllers` | REST endpoints. Events from HTTP requests are published here, after the transaction commits. |
| `src/main/kotlin/.../services` | Business logic, Prison API and Prisoner Search clients, event handling, the nightly job. |
| `src/main/kotlin/.../batch` | Batch mode for the nightly CronJob. |
| `src/main/resources/db/migration` | Flyway migrations (one table). |
| `src/main/resources/sar` | Subject access request template. |
| `async-api.yml` | AsyncAPI description of the published events. |
| `helm_deploy/` | Helm chart and per-environment values, including the CronJob and alerts. |
| `docs/` | You are here. |

## Glossary

| Term | Meaning |
|---|---|
| Restricted patient | A prisoner moved to hospital who is still the responsibility of a prison. |
| Supporting prison | The prison responsible for the patient, where their Prison Offender Manager sits. Starts as the prison they left; can be changed. |
| Hospital | A NOMIS agency of type `HOSPITAL` or `HSHOSP` (secure hospital). There is no separate hospital list. |
| `REL` / `HP` | NOMIS movement type "release" and reason "to hospital". The movement that makes someone a restricted patient. |
| `CR` | Conditional release. The reason used on the movement booked when someone is removed. |
| CRD / SED | Conditional release date / sentence expiry date. Used by the nightly job. |
| DPS066 | The service's product id in the Developer Portal and on the Live Service Confluence pages. |
