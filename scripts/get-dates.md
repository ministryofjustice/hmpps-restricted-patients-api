# Get Offender Dates

## Overview

We need to know the offenders in Restricted Patients and their release dates to help us reason about their real status.

## Inputs

First connect to the Restricted Patients database using the credentials found in the prod namespace secrets. Run the following SQL to retrieve a list of offender numbers.

```sql
select prisoner_number  from restricted_patients
```

Then connect to the NOMIS database using the credentials as found in prison-api namespace secrets. Run the following SQL to retrieve the booking IDs, inserting the previous list of offender numbers:

```sql
SELECT offender_id_display, ob.OFFENDER_BOOK_ID  FROM OMS_OWNER.OFFENDERS o 
JOIN OMS_OWNER.OFFENDER_BOOKINGS ob ON o.OFFENDER_ID = ob.OFFENDER_ID 
WHERE booking_seq=1 AND OFFENDER_ID_DISPLAY in (
    'A1234AA',
    'A12345BB',
    ...
    )
```

Export the results as `offenders.csv` in this directory.

## Authenticating

In order to retrieve the sentence dates we will call prison-api endpoint `api/bookings/$bookingId/sentenceDetail`. This in turn calls BookingService#getSentenceCalcDates which currently requires one of the following roles: SYSTEM_USER, GLOBAL_SEARCH, VIEW_PRISONER_DATA.

To authorise this call you need to get a JWT from the Auth server using your personal client credentials which must have one of the roles mentioned above. For more details on retrieving a token from Auth see its [README](https://github.com/ministryofjustice/hmpps-auth#using-client-credentials).

## Running the script

You should now have a list of offender numbers and booking IDs in `offenders.csv` and an Auth token. To get the dates run the script:

```bash
./get-dates.sh <insert-auth-token-here>
```

This will produce an output file called `results.csv`.

Note that the script only calls prison-api once per second to try not to DoS that service. Therefore the script will take a long time - e.g. 900 offenders takes over 15 minutes. You should also make sure your token does not expire before the script has finished.

Any failures by the script will be reported to the terminal.
