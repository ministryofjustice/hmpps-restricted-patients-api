#!/bin/bash

TOKEN=$1
ENV=$2

if [ -z "$2" ]
then
  ENV=dev
fi

ENV_SUFFIX=""
if [ "$ENV" != "prod" ]
then
  ENV_SUFFIX="-$ENV"
fi

DRY_RUN_RESULTS=$(curl -s --location --request GET "https://restricted-patients-api$ENV_SUFFIX.hmpps.service.justice.gov.uk/dryrun-unknown-patients" \
  --header "Authorization: Bearer ${TOKEN}")
DRY_RUN_FAIL_COUNT=$(echo "$DRY_RUN_RESULTS" | jq 'map(select(.success == false)) | length')
if [ "$DRY_RUN_FAIL_COUNT" -gt 0 ]
then
  echo "$DRY_RUN_RESULTS" | jq .
  exit 1
fi

for unknownPatient in $(< unknown-patients.csv);
do

  if [[ $unknownPatient ==  FILE* ]]
  then
    continue
  fi

  RESULTS=$(curl -s --location --request POST "https://restricted-patients-api$ENV_SUFFIX.hmpps.service.justice.gov.uk/process-unknown-patients" \
    --header "Authorization: Bearer ${TOKEN}")
  REF=$(echo "$RESULTS" | jq '.[] | .mhcsReference' | tr -d '"')
  SUCCESS=$(echo "$RESULTS" | jq '.[] | .success')
  echo "$RESULTS" > "process_unknown_$REF_$SUCCESS.json"

  sleep 1
done
