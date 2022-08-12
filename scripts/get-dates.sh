#!/bin/bash

TOKEN=$1

echo "nomsNumber,bookingId,conditionalReleaseDate,licenceExpiryDate,sentenceExpiryDate" > results.csv

for offenderDetails in $(cat offenders.csv);
do
  IFS=, read -r -a details <<< "$offenderDetails"
  nomsNumber=${details[0]}
  bookingId=${details[1]}

  RESULTS=$(curl -s --location --request GET "https://api.prison.service.justice.gov.uk/api/bookings/$bookingId/sentenceDetail" \
    --header "Authorization: Bearer ${TOKEN}")

  if [[ -z "${RESULTS}" ]]; then
    echo "Failed to receive a result for nomsNumber=${nomsNumber}, bookingId=${bookingId}"
  fi

  conditionalReleaseDate=$(echo $RESULTS | jq .conditionalReleaseDate)
  licenceExpiryDate=$(echo $RESULTS | jq .licenceExpiryDate)
  sentenceExpiryDate=$(echo $RESULTS | jq .sentenceExpiryDate)

  echo "${nomsNumber},${bookingId},${conditionalReleaseDate},${licenceExpiryDate},${sentenceExpiryDate}" >> results.csv
  sleep 1
done
