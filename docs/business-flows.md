# Business flows

What happens, step by step, when someone becomes or stops being a restricted patient. For the
components and events named here see [architecture.md](architecture.md).

## What a restricted patient is

A restricted patient is a prisoner who has been moved from prison to a hospital, usually a
secure hospital under the Mental Health Act, but who is still the responsibility of a prison.
In NOMIS they have been *released*: their last movement is a release (`REL`) with reason
"to hospital" (`HP`), so NOMIS no longer shows them in any prison. This service keeps track of
them so that:

- the **supporting prison** (where their Prison Offender Manager sits) can still find them, see
  their profile and add case notes;
- prisoner search can show them as restricted patients rather than simply "released";
- they are removed automatically when their custody ends or when they come back to prison.

A person can be added to this service in three ways and removed in three ways.

| Becomes a restricted patient | Stops being a restricted patient |
|---|---|
| [Moved to hospital using this service](#move-a-prisoner-to-hospital) | [Removed by a member of staff](#remove-a-restricted-patient) |
| [Added after being released to hospital in NOMIS](#add-a-prisoner-already-released-to-hospital) | [Received back into prison](#received-back-into-prison) |
| [Picked up automatically from a NOMIS discharge](#discharge-done-directly-in-nomis) | [Removed by the nightly job when custody ends](#nightly-removal-when-custody-ends) |

The supporting prison can also be [changed](#change-supporting-prison). There is no way to change
the hospital; the patient would have to be removed and added again.

## Move a prisoner to hospital

UI journey `/move-to-hospital`, role `ROLE_TRANSFER_RESTRICTED_PATIENT`.

1. The user searches for a prisoner. Results are limited to the user's **active caseload**, and
   prisoners past their conditional release date (or recalls past sentence expiry) are left out.
2. The user picks the hospital from the Prison API agency list (types `HOSPITAL` and `HSHOSP`).
3. On confirm, the UI calls `POST /discharge-to-hospital` with the user's token, sending the
   prisoner number, the prison they are in and the hospital.
4. The API, in one database transaction:
   1. refuses if the prisoner is already a restricted patient (`EXISTING_PATIENT`);
   2. checks Prison API for an active booking (404 if none);
   3. saves the row, with the supporting prison defaulting to the current prison and the
      discharge time to now;
   4. calls Prison API `PUT /api/offenders/{no}/discharge-to-hospital`, as the user. **Prison API
      creates the release-to-hospital movement in NOMIS.** If this fails the row is rolled back.
5. After the commit the API publishes `restricted-patients.patient.added`. Prisoner search
   re-indexes the prisoner.

## Add a prisoner already released to hospital

UI journey `/add-restricted-patient` ("Add a released prisoner into restricted patients"), role
`ROLE_RESTRICTED_PATIENT_MIGRATION`. This is for people released to hospital in NOMIS who are not
yet in this service. It was added in May 2022 (NN-4127) to bring across patients released in NOMIS,
and is still the fix when the [automatic pick-up](#discharge-done-directly-in-nomis) does not happen.

1. The user searches prisoners whose location is `OUT`. People already in the service are left
   out; anyone whose last movement is not a release to hospital (`REL` with reason `HP` or `HO`),
   or who is past their release date, is shown as not eligible.
2. The user picks the hospital.
3. The UI calls `POST /migrate-in-restricted-patient`.
4. The API checks the latest NOMIS movement: there must be exactly one, it must be a release
   (`REL`), and it must have a "from" prison. It takes the discharge time and prison from that
   movement and records a fixed comment ("Historical discharge to hospital added to restricted
   patients").
5. It then calls the same Prison API discharge-to-hospital endpoint as the move journey, and
   publishes `added`.

## Discharge done directly in NOMIS

No UI involved. Some prisons release to hospital in NOMIS rather than using this service.

1. Prisoner search publishes `prisoner-offender-search.prisoner.released` with reason
   `RELEASED_TO_HOSPITAL`.
2. The API checks the prisoner's last NOMIS movement is a release with reason `HP` and that the
   destination is a hospital agency (`HOSPITAL` or `HSHOSP`). If not, it logs an error and does
   nothing.
3. If the prisoner is not already a restricted patient it saves the row, with the supporting
   prison set to the prison they left, and publishes `added`. Nothing is written to NOMIS.

## Change supporting prison

UI journey `/change-supporting-prison`, role `ROLE_RESTRICTED_PATIENT_MIGRATION`.

1. The user finds the patient and picks a prison.
2. The UI calls `POST /change-supporting-prison`.
3. The API refuses if the prison is unchanged (`PRISON_UNCHANGED`) or unknown
   (`PRISON_NOT_FOUND`), then updates the row and publishes
   `restricted-patients.patient.supporting-prison-changed`.

**This changes this service only. Nothing is written to NOMIS.** Prisoner search picks up the new
supporting prison from the event, which in turn changes which prison's staff can find the patient.

## View restricted patients

UI journey `/view-restricted-patients`, role `ROLE_SEARCH_RESTRICTED_PATIENT`.

Searches prisoner search's restricted patient index (`/restricted-patient-search/match-restricted-patients`),
not this API, and links to the prisoner profile and to "Add a case note". A search term containing
a digit is treated as a prisoner number; otherwise it is read as "last name first name".

## Remove a restricted patient

UI journey `/remove-from-restricted-patients`, role `ROLE_REMOVE_RESTRICTED_PATIENT`.

1. The user finds the patient and confirms.
2. The UI calls `DELETE /restricted-patient/prison-number/{no}` (with a system token).
3. The API looks the prisoner up in prisoner search for the booking id, deletes the row and calls
   Prison API `POST /api/movements` to book a release from the hospital to `OUT`
   (`REL`, reason `CR`, conditional release). If the NOMIS call fails the delete is rolled back.
4. After the commit it publishes `restricted-patients.patient.removed`.

There is no reason for removal; the release movement always uses reason `CR`.

## Received back into prison

No UI involved.

1. NOMIS raises `OFFENDER_MOVEMENT-RECEPTION` on the prison offender events topic.
2. If the prisoner is a restricted patient the API deletes the row and publishes `removed`.
   Nothing is written to NOMIS, as the reception is already there.

## Nightly removal when custody ends

No UI involved. The `past-date-check` job (03:00 in preprod and prod) looks up every restricted
patient in prisoner search and removes those who are not on an indeterminate sentence and are
either past their conditional release date (not recalled) or past their sentence expiry date
(recalled). Each removal follows the [manual removal](#remove-a-restricted-patient) path, using
the system token, and publishes `removed`. See [architecture.md](architecture.md#nightly-clean-up-job).

Sentence data in NOMIS is sometimes missing or wrong, so the job leaves anyone it is not sure about.
That is deliberate: a missing date or a null indeterminate flag means "do not remove".

## Merged prisoner records

When two NOMIS records are merged and either one is a restricted patient, the API does not try to
work out the right answer. It fails the message on purpose so it lands on the dead letter queue,
and someone has to resolve it by hand. See
[runbook.md](runbook.md#a-restricted-patient-has-been-merged).
