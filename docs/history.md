# History

How the service got to where it is. Useful when a ticket key in a commit message means nothing to
you, or when you are wondering why something was done a particular way.

## Who has looked after it

| Period | Team | Jira key in commits |
|---|---|---|
| 2021 to mid 2022 | The DPS team that built it (and went on to build Manage Adjudications, which is why the namespace's Slack channel was `dps_adjudications`) | `NN-` |
| Mid 2022 to October 2026 | Syscon | `SDI-` (2022), then `SDIT-` from March 2023. Old `SDI-` keys redirect to `SDIT-`. |
| From October 2026 | Move a Prisoner, Squad B | `MAPB-` |

Other keys you will see: `DT-` (DPS tech), `HAAR-` (HMPPS Auth and Audit), `HEAT-` (HMPPS
developer tooling), `CDPS-` (Connect DPS). Most commits on the UI repository without a key are
merges from the HMPPS TypeScript template.

Squad B took over on 26 to 27 October 2026 (MAPB-1040), with product ownership agreed by the
Move a Prisoner product leads. Syscon had supported the service day to day, keeping libraries
and infrastructure up to date and handling support tickets.

## Timeline

### 2021: built

- **Jun 2021**: API created from the HMPPS Kotlin template. Discharge to hospital (NN-3463).
- **Jul 2021**: UI built: search, move to hospital, remove. Preprod and prod set up (NN-3466).
- **Aug 2021**: Postgres, prisoner number as the primary key (NN-3595), domain events published and
  consumed (NN-3574, NN-3588). The schema has not changed since.

### 2022: in service

- **Jan 2022**: moved to the live Cloud Platform cluster (NN-3922). Preprod database refresh from
  prod set up (NN-3931).
- **May 2022**: `migrate-in-restricted-patient` added for patients already released to hospital in
  NOMIS (NN-4127).
- **Jun to Aug 2022**: Syscon take over (first commit SDI-240, June 2022). Listening for
  `OFFENDER_MOVEMENT-RECEPTION` to remove patients received back into prison (SDI-331).
- **Sep 2022**: UI journey "Add a released prisoner into restricted patients" (SDI-345) and the help
  pages (SDI-364).
- **Oct to Nov 2022**: migration of unknown NOMIS patients (SDI-357); merge detection (SDI-432);
  automatic removal of patients past their release date, as a cron job (SDI-350). The cron job was
  created suspended and did not actually run until May 2024 (SDIT-1718).

### 2023

- **Jan 2023**: Spring Boot 3 (SDI-516).
- **Sep to Nov 2023**: DPS header and footer from frontend components (SDIT-1108). Moved to the new
  prisoner search (SDIT-1114, SDIT-1146). The API stopped calling the search indexer directly and
  publishes events instead (SDIT-1157, SDIT-1159). Own queues for offender and domain events
  (SDIT-670).

### 2024

- **Jan 2024**: discharges done directly in NOMIS picked up automatically from
  `prisoner-offender-search.prisoner.released` (SDIT-1413).
- **Feb 2024**: subject access request endpoint (SDIT-1518); moved to the HMPPS Kotlin starter
  (SDIT-1335).
- **May 2024**: nightly removal job finally switched on (SDIT-1718). Dev switched off overnight
  (SDIT-1646).
- **Sep 2024**: change supporting prison, API and UI (SDIT-2003, SDIT-2010). Prison and hospital
  descriptions in responses (SDIT-2036).
- **Oct 2024**: person reference in events and AsyncAPI documentation (SDIT-1841).

### 2025

- **May 2025**: CircleCI replaced by GitHub Actions and Cloud Platform's hmpps-template module
  (SDIT-2737).
- **Sep 2025**: alerts moved from #dps_alerts to Syscon's channels (SDIT-3023).
- **Oct 2025**: Java 25 (SDIT-3072). Nightly job moved to the shared CronJob template (SDIT-2804),
  which broke it until November (SDIT-3217). Exponential back-off on queue retries (SDIT-3046).
- **Dec 2025**: Spring Boot 4 (SDIT-3215), Postgres 18 (SDIT-3181).

### 2026

- **Feb to Mar 2026**: duplicate active NOMIS movements on removal, fixed in Prison API and repaired
  (SDIT-3454, SDIT-3462). Retry cron job for dead letter queues removed (SDIT-3616).
- **Apr 2026**: ModSecurity on both apps (SDIT-3654).
- **May 2026**: API moved into the shared `hmpps-restricted-patients-*` namespaces and its old
  namespaces deleted (SDIT-3675, SDIT-3810).
- **Jun to Sep 2026**: subject access request fields agreed (MAPB-358, MAPB-831). Postgres 18.6
  security upgrade (SDIT-4231).
- **Oct 2026**: generic-prometheus-alerts 2.x, so alert channels now come from the Developer Portal
  (SDIT-4333). User UUID in UI telemetry (MAPB-1038), UI page views sent to HMPPS Audit
  (MAPB-1039), and hand-over to Move a Prisoner (MAPB-1040). Roles required on every endpoint
  (MAPB-1049, MAPB-1052); legacy DPS stopped calling the API.

## Further reading

Confluence holds service and domain background but no developer documentation:

- *DPS066 - Restricted Patients* (HMPPS Digital Live Service, page 4955766785): what a restricted
  patient is, and what must be true in NOMIS before someone can be moved.
- *Restricted Patients* (Move a Prisoner space, page 6008178569): legal background (sections 37/41,
  47/49, 48/49 and 45A of the Mental Health Act, and the Mental Health Casework Section).
- *How do users get access - DPS066* (page 4955766805).
- *Restricted Patients - Events - Development* (page 4319182952): how to move test prisoners in and
  out of prison in dev NOMIS.
