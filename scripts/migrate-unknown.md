# Migrate Unknown Patients into Restricted Patients

## Overview

We have many patients who do not have NOMIS records but need adding to Restricted Patients so that they are available to MPC.

## Pre-requisites

Obtain a copy of the spreadsheet containing these patients - currently named `15922 MHCS RP data no queries`. Export the worksheet `MHCS data included` as a CSV file and copy it into this directory with file name `unknown-patients.csv`.

The script needs a valid HMPPS-Auth token with role `ROLE_RESTRICTED_PATIENT_MIGRATION` and a NOMIS user in the `user_name` claim (achieved by adding `?username=<enter-username-here>` to the Access Token URL).

The case note type/sub-type `MIGRATION`/`OBS_GEN` needs activating in production NOMIS. You can request this in the `#ask_prison_nomis` Slack channel, and when the migration is complete it should be turned off again.

## Method

Take the HMPPS-Auth token generated in #pre-requisites and save it to an env var (e.g. $TOKEN in the below examples).

First perform a dry-run to make sure the input data is valid:

```bash
./migrate-unknown.sh -t $TOKEN -e prod
```

This will create a directory called `prod` which contains a results file for each line in the CSV file. Any that fail will have suffix `...-false.json` - investigate these to see why they failed.

When all CSV records pass the dry-run perform the real migration with the following command:

```bash
./migrate-unknown.sh -t $TOKEN -e prod --no-dry-run
```

This will overwrite the directory `prod` with the results. Again check for any failures.

## Handling failures

If any of the API calls fail then the migration of that patient will be aborted and the error message written to the results file. It's hard to predict what might go wrong as all obvious failures have been ironed out during testing.

Should any migrations fail then you'll have to investigate and work out why. Once fixed you can re-run the migration for that patient if the previous migration failed while trying to create a prisoner. If the prisoner record was created (i.e. an `offenderNumber` is returned in the results file) then you'll have to delete the NOMIS offender record before attempting the migration again.
