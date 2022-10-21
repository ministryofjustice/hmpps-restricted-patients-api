#!/bin/bash

#set -x

TOKEN=none
ENV=dev
DRY_RUN=1

while [[ $# -gt 0 ]]; do
  case $1 in
    -t)
      TOKEN=$2
      shift # past argument
      shift # past value
      ;;
    -e)
      ENV=$2
      shift # past argument
      shift # past value
      ;;
    --no-dry-run)
      DRY_RUN=0
      shift # past argument
      ;;
  esac
done

ENV_SUFFIX=""
if [ "$ENV" != "prod" ]
then
  ENV_SUFFIX="-$ENV"
fi

rm -rf $ENV
mkdir $ENV

if [ $DRY_RUN -eq 1 ]
then
  REQ_PATH="dryrun-unknown-patient"
  echo "This is a dry run."
else
  REQ_PATH="process-unknown-patient"
fi

while read -r unknownPatient
do

  if [[ $unknownPatient ==  FILE* ]]
  then
    continue
  fi

  RESULT=$(curl -sfS --http1.1 -X POST "https://restricted-patients-api$ENV_SUFFIX.hmpps.service.justice.gov.uk/$REQ_PATH" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$unknownPatient" \
    2>&1)
  STATUS="$?"

  if [ "$STATUS" -eq 0 ]
  then
    REF=$(echo "$RESULT" | jq '.mhcsReference' | tr -d '"' | sed 's/\//-/g')
    SUCCESS=$(echo "$RESULT" | jq '.success')
    RESULT_FILE="process-unknown-$REF-$SUCCESS.json"
    echo "$RESULT" | jq . > "$ENV/$RESULT_FILE"
    echo "$ENV/$RESULT_FILE"
  else
    REF=$(cut -d ',' -f 1 <<< "$unknownPatient" | sed 's/\//-/g')
    echo "$RESULT" > "$ENV/process-unknown-$REF-ERROR.json"
    echo "Error for $REF"
  fi

  sleep 1
done < unknown-patients.csv
