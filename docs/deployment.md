# Deployment and infrastructure

Where the service runs, how it is released, and where its alerts go.

## Environments

| | dev | preprod | prod |
|---|---|---|---|
| UI | <https://manage-restricted-patients-dev.hmpps.service.justice.gov.uk> | <https://manage-restricted-patients-preprod.hmpps.service.justice.gov.uk> | <https://manage-restricted-patients.hmpps.service.justice.gov.uk> |
| API | <https://restricted-patients-api-dev.prison.service.justice.gov.uk> | <https://restricted-patients-api-preprod.prison.service.justice.gov.uk> | <https://restricted-patients-api.prison.service.justice.gov.uk> |
| Namespace | `hmpps-restricted-patients-dev` | `hmpps-restricted-patients-preprod` | `hmpps-restricted-patients-prod` |
| Replicas (each app) | 2 | 2 | 4 |
| Switched off overnight | Yes, weekdays 21:4x to 06:2x UTC and all weekend | UI only, same hours | No |
| Deploy approval | None | `map-developers-live` | `map-developers-live` |

The API is reachable only from internal networks. The UI is open to MoJ staff, prison and
Cloud Platform networks. Both sit behind ModSecurity (paranoia level 3); blocked requests return
**406**, and blocked request logs are tagged `github_team=map-developers-devs` in OpenSearch.

Developer Portal: <https://developer-portal.hmpps.service.justice.gov.uk/products/restricted-patients>
(product `DPS066`, team Move a Prisoner, Jira project MAPB).

## Releasing

Both repositories use the shared `hmpps-github-actions` pipeline (`.github/workflows/pipeline.yml`):
tests and helm lint on every branch; on `main`, build and sign the image, push to ghcr, then deploy
to dev, preprod and prod in turn. Preprod and prod wait for approval from `map-developers-live`.
`deploy_to_env.yml` redeploys an existing version by hand.

Release the API before the UI when a UI change depends on a new API endpoint.

## Infrastructure

Defined in [cloud-platform-environments](https://github.com/ministryofjustice/cloud-platform-environments/tree/main/namespaces/live.cloud-platform.service.justice.gov.uk),
under `hmpps-restricted-patients-{env}`. Until May 2026 the API had its own namespaces
(`hmpps-restricted-patients-api-{env}`); they were merged into one per environment under SDIT-3675.

| Resource | Used by | Notes |
|---|---|---|
| RDS Postgres 18 (`hmpps-restricted-patients-api-{env}`) | API | `db.t4g.small`. Connection details in secret `rp-rds-instance-output`. Dev is stopped and started automatically. |
| ElastiCache Redis 7 | UI | Sessions and cached system tokens. Secret `elasticache-redis`. |
| SQS `dps-core-{env}-restricted_patients_domain_queue` (+ `_dlq`) | API | Subscribed to the HMPPS domain events topic, filter `prison-offender-events.prisoner.merged`, `prisoner-offender-search.prisoner.released`. |
| SQS `dps-core-{env}-restricted_patients_offender_queue` (+ `_dlq`) | API | Subscribed to the offender events topic, filter `OFFENDER_MOVEMENT-RECEPTION`. |
| HMPPS domain events topic | API | Publish only, through service account `hmpps-restricted-patients-api`. Topic ARN in secret `hmpps-domain-events-topic`. |
| HMPPS Audit queue | UI | Send only, through service account `hmpps-restricted-patients-ui`. Secret `sqs-hmpps-audit-secret`, created by the hmpps-audit namespaces. |
| GitHub environments and secrets | Both | `hmpps-restricted-patients-github.tf` (`cloud-platform-terraform-hmpps-template`) creates the GitHub environments, reviewers and deploy credentials. |

**Do not change `team_name` (`dps-core`) in the namespace `variables.tf`.** The queue names are
built from it, so changing it would delete and recreate all four queues and their subscriptions.

Secrets not created by Terraform (set by hand): the API's HMPPS Auth client
(`hmpps-restricted-patients-api-client-creds`) and the UI's `-auth-code`, `-client-creds` and
`-session-secret`.

Every plan in these namespaces currently shows an update to `module.rp_rds.aws_db_parameter_group`
and, in prod, a replacement of the domain events topic policy attachment. Both are existing drift.

## Scheduled jobs

| CronJob (prod name) | Schedule | What it does |
|---|---|---|
| `api-past-date-check` | 03:00 daily, Europe/London (dev: 15:00 weekdays) | The nightly clean-up. See [architecture.md](architecture.md#nightly-clean-up-job). |
| `api-postgres-restore` | Every 30 minutes, 06:00 to 21:00, Sunday to Friday | From the generic-service chart (`postgresDatabaseRestore`). Checks whether NOMIS preprod has been refreshed since the last restore and, if so, copies the prod database to preprod so the two stay in step. Uses the `rp-rds-instance-output-preprod` secret that preprod writes into the prod namespace. |

## Alerts

Alerts come from `generic-prometheus-alerts` 2.x. Since SDIT-4333 (October 2026) the Slack
channel is not set in helm: the alert relay sends alerts to the channels recorded against each
component in the Developer Portal.

| Alert type | Channel |
|---|---|
| Production | #move-a-prisoner-alerts |
| Non-production (business hours only) | #move-a-prisoner-alerts-nonprod |
| Security scans (CodeQL, Snyk, Veracode, OWASP) | MAP security channel, set by repository variable `SECURITY_ALERTS_SLACK_CHANNEL_ID` |

What raises an alert:

- **Any message on any of the four queues** (`sqsAlertsTotalMessagesThreshold: "0"`). The value
  must stay the string `"0"`; otherwise the chart default of 100 applies.
- Pod restarts, error rates and response times (chart defaults).
- ElastiCache health for the UI's Redis.
- RDS health for the API database (`rdsAlertsDatabases` in the API's `values-prod.yaml`).
