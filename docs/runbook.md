# Runbook

What to do when something goes wrong, and procedures used in the past. Day to day the service
needs no manual work. Most entries come from Syscon's support of the service between 2022 and
October 2026; the ticket for each is given so the detail can be found.

**Related docs:** [architecture.md](architecture.md) · [business-flows.md](business-flows.md) ·
[deployment.md](deployment.md)

---

## Where to look

| What | Where |
|---|---|
| API logs and telemetry | App Insights, `cloud_RoleName == 'hmpps-restricted-patients-api'` |
| UI logs and telemetry | App Insights, `cloud_RoleName == 'hmpps-restricted-patients-ui'` |
| Pods, jobs, secrets | `kubectl -n hmpps-restricted-patients-<env> ...` (Move a Prisoner developers have admin) |
| Queues | `kubectl` plus the API's `/queue-admin` endpoints, or the AWS console |
| Blocked requests | OpenSearch ModSecurity logs, `github_team=map-developers-devs` |

Useful custom events are listed in [architecture.md](architecture.md#telemetry-worth-knowing).

---

## A message is stuck on a dead letter queue

Any message on any of the four queues raises an alert. Messages are retried 5 times with back-off
and then sit on the dead letter queue. **Nothing retries them automatically** (the retry cron job
was removed in SDIT-3616).

1. Find the failure in App Insights:
   ```
   traces
   | where cloud_RoleName == 'hmpps-restricted-patients-api'
   | where severityLevel >= 3
   | order by timestamp desc
   ```
2. Work out which case it is:
   - **"Merge not implemented"**: see [the next section](#a-restricted-patient-has-been-merged).
   - **A downstream service was unavailable** (Prison API, Prisoner Search): retry once it is back.
   - **A field was missing from the event** (for example no `reason` on a release event): look at the
     message, decide whether it matters, then purge or retry.
3. Retry or purge with the `hmpps-sqs` queue admin endpoints on the API. They need a token with
   `ROLE_SYSCON__QUEUE_ADMIN`:
   - `GET /queue-admin/get-dlq-messages/{dlqName}` to see what is there
   - `PUT /queue-admin/retry-dlq/{dlqName}` to move them back to the main queue
   - `PUT /queue-admin/purge-queue/{queueName}` to throw them away

   Queue names are `dps-core-<env>-restricted_patients_{domain,offender}_{queue,dlq}`.

## A restricted patient has been merged

Merges of two NOMIS records are detected but deliberately not handled. Merges are rare, a
restricted patient is still serving a sentence so should not normally be merged, and the logic
would be complicated. The API throws, the message ends up on the domain events dead letter queue,
and someone has to sort it out.

1. Find the message in App Insights (see above). It reads:
   `Merge not implemented. Patient A1234BC was at hospital ESURRY but record merged into B2345CD`
2. Decide what the right state is:
   - **The merged person is now in prison**: delete the restricted patient row. It may be under
     either prisoner number, depending on which was the older.
   - **The merged person is outside prison**: look at the movements and sentence to work out
     whether they should still be in hospital or have been released.
     - Released: delete the row.
     - Still in hospital: change the row to the surviving prisoner number, or delete one of the rows
       if they were in hospital under both numbers.
   - **The merge was done the wrong way round** (an old booking is now the latest, rather than the
     move to hospital): raise it in #ask-prison-nomis. A script may be needed to put the bookings in
     the right order.
3. **Re-index the prisoner in prisoner search afterwards** (see below). Search, the UI and DPS do
   not see a change made directly in the database until you do.
4. Purge the message from the dead letter queue.

Example: SDIT-1057 (A5937AC, August 2023).

## Re-indexing a prisoner after a database change

Anything that changes the `restricted_patients` table directly, rather than through the API,
publishes no event, so prisoner search keeps the old details. Re-index the prisoner with the
prisoner search indexer's `PUT /maintain-index/index-prisoner/{prisonerNumber}`, which needs
`ROLE_PRISONER_INDEX`. If you do not have a client with that role, ask the prisoner search team.

## Someone released to hospital in NOMIS is not in Restricted Patients

From the Live Service runbook "Prison user cannot find an offender in restricted patients"
(Confluence 5877268724).

The API only picks up a NOMIS release automatically when the last movement is a release with reason
`HP` to an agency of type `HOSPITAL` or `HSHOSP`, and the prisoner is not already a restricted
patient. Otherwise it logs `Ignoring release of prisoner ...` and does nothing.

1. Ask a user with the **CADM Restricted Patient Migration** role
   (`ROLE_RESTRICTED_PATIENT_MIGRATION`) to use "Add a released prisoner into restricted patients"
   in the UI.
2. If the hospital is not in the list, NOMIS Applications Support need to add it as an agency.
   Hospitals are NOMIS agencies; this service has no list of its own.
3. Manage POM can take about an hour to show the person afterwards.

## The nightly job did not run, or failed

```bash
kubectl -n hmpps-restricted-patients-prod get cronjobs
kubectl -n hmpps-restricted-patients-prod get jobs --sort-by=.status.startTime
kubectl -n hmpps-restricted-patients-prod logs job/<job-name>
```

- Check `SUSPEND` is `False` and `LAST SCHEDULE` is recent. In 2024 the job had been suspended
  since it was created, and had not run for months (SDIT-1718).
- A successful run raises the custom event `restricted-patient-batch-removal` (with the prisoner
  numbers removed) in App Insights. Individual failures are logged as
  `restricted-patient-batch-removal failed for offender ...` and do not stop the run.
- To run it again now: `kubectl -n hmpps-restricted-patients-prod create job --from=cronjob/api-past-date-check past-date-check-manual-<date>`.

SDIT-3217 (November 2025): after the job was moved to the shared CronJob template it ran without an
HTTP request, and failed because it was using the request-scoped Prison API client. Fixed by giving
batch mode the system clients. Any new code used by the job must not depend on a request.

## Duplicate active movements after removing a restricted patient

SDIT-3454 and SDIT-3462 (February to March 2026).

Removing someone from restricted patients books a new release movement in NOMIS. Until February
2026, Prison API left the earlier release-to-hospital movement active as well, so the booking had
two active external movements. Prison API was fixed (prison-api #2588, "Mark previous external
movements as inactive when creating new movement").

Existing bookings were repaired with Prison API's
`POST /api/prisoner-repair/{bookingId}/restricted-patient-movements` (role
`PRISON_API__PRISONER_REPAIR__RW`), which checks the last active movement is a `CR` release and the
one before is an `HP` release. The SQL used to find affected bookings is on SDIT-3462. If duplicates
appear again, use the same query and endpoint.

## A user needs access

From the Live Service page "How do users get access - DPS066" (Confluence 4955766805).

| Role (as shown to admins) | Role code | Granted by |
|---|---|---|
| Restricted Patient Transfer | `TRANSFER_RESTRICTED_PATIENT` | Local system administrator |
| Restricted Patient Removal | `REMOVE_RESTRICTED_PATIENT` | Local system administrator |
| Restricted Patient Search | `SEARCH_RESTRICTED_PATIENT` | Central admin |
| CADM Restricted Patient Migration | `RESTRICTED_PATIENT_MIGRATION` | Central admin, or developers |

## Other known issues

- **Returning from hospital on a new booking** loses the court cases from the old booking, and there
  is no way to copy them across. Owned by the court cases and release dates team (CRDS-1096); not a
  fault in this service.
- **Pathfinder shows restricted patients as "inside"** (DSE-1204, in Pathfinder's backlog).
- **dps-reconciliation** used to report false positives for restricted patient events; fixed in that
  service (SDIT-3156, SDIT-3252).
