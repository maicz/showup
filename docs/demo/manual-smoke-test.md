# ShowUp primary-journey manual smoke test

Run this procedure against a candidate environment that has the resettable fixtures defined in [the release-demo contract](release-contract.md). Record the date, environment URL, browser/device, candidate commit or image digest, and screenshots or recording location in the release evidence directory.

## Preconditions

- The environment is explicitly identified as local, preview, or production-like; do not represent a local run as public proof.
- `demo-organizer`, `demo-confirmed`, and `demo-waitlisted` exist with credentials supplied through the environment’s secure demo-fixture channel.
- The `Release Journey Lab` event is published, capacity one, waitlist enabled, and no RSVP has yet been submitted.
- Test with a private/incognito browser context where practical to avoid a stale authenticated session.

## Procedure and expected result

| Step | Actor | Action | Expected result |
| --- | --- | --- | --- |
| 1 | Visitor | Browse public events and open `Release Journey Lab`. | The event is visible, published, and has a clear sign-in/RSVP route. |
| 2 | Confirmed attendee | Sign in and RSVP `YES`; open the ticket. | RSVP status is `YES`; one ticket/QR code is available. |
| 3 | Waitlisted attendee | Sign in and RSVP `YES`. | RSVP status is `WAITLISTED` at position `1`; no usable admission ticket is issued. |
| 4 | Confirmed attendee | Cancel the RSVP. | Cancellation succeeds. |
| 5 | Waitlisted attendee | Refresh/open the RSVP state. | Status becomes `YES`; waitlist position is absent; ticket can be opened. |
| 6 | Organizer | Open the event’s check-in screen and manually enter the promoted attendee’s ticket code. | One admission succeeds and turnout changes by one. |
| 7 | Organizer | Submit the same code again. | Duplicate is safely identified; total admissions do not increase. |
| 8 | Organizer | Open the attendance report. | Registered `1`, attended `1`, no-shows `0`, attendance `100%`; one staff scan total. |
| 9 | Visitor | Reopen the event anonymously. | Public details remain visible; attendee-only ticket and organizer controls are not exposed. |

## Pass criteria

Pass only when every expected result is observed, keyboard navigation can reach each required control, and no browser-console/API error affects the journey. Mark the smoke test failed if the flow needs manual data repair, hidden administrative steps, or a retry that changes the evidence state.

## Evidence to retain

- A dated walkthrough recording or screenshot set with secrets, ticket codes, and personal data redacted.
- Browser/device/version and candidate commit or image digest.
- Any failure, workaround, or known limitation with its follow-up issue.
