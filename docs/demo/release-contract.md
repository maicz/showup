# ShowUp release-demo contract

**Status:** Release scope locked; local development evidence only.  
**Primary journey:** discover a public group and event, register as an attendee, RSVP under capacity/waitlist rules, obtain an admission ticket, check in through an authorized organizer, and inspect the resulting attendance report.

This contract defines what the ShowUp release must prove. It is deliberately narrower than the full product surface, so an evaluator can follow one dependable story from discovery to operations evidence.

## Roles and permitted actions

| Persona | Purpose in the demo | Permitted actions |
| --- | --- | --- |
| Visitor | Establish that discovery works without an account. | Browse public groups and published events; inspect public event details. |
| Organizer | Operate the event and prove access control. | Create a public group and venue; create/publish the event; access check-in and reporting. |
| Confirmed attendee | Exercise the admission path. | Register, RSVP `YES`, receive/view one deterministic ticket, and attend. |
| Waitlisted attendee | Demonstrate capacity integrity. | Register, request RSVP after capacity is exhausted, see position, then become confirmed after a cancellation. |
| Door staff | Optional production fixture role. | Scan/check in only after explicit staff assignment; it must not inherit organizer authority. |

The manual walkthrough may use the organizer for the scanner step until a repeatable staff-assignment fixture exists. That exception must be visible in demo notes; it is not evidence that the staff path has been validated.

## Fixture plan

Fixtures must be non-sensitive, disposable, and resettable. Do not use a real person’s name, email, ticket, venue, or photo.

| Fixture | Required state | Why it exists |
| --- | --- | --- |
| `demo-organizer` | Organizer of the public `ShowUp Engineering Nights` group. | Owns publishing, check-in, and reporting. |
| `demo-confirmed` | Confirmed RSVP for `Release Journey Lab`; ticket issued. | Shows the attendee/ticket path. |
| `demo-waitlisted` | Waitlisted for the same capacity-one event until `demo-confirmed` cancels. | Makes promotion observable. |
| `Release Journey Lab` | Published, future, in-person event; capacity `1`; waitlist enabled; zero guest allowance; non-production venue. | Keeps capacity and ticket transitions deterministic. |
| `Demo Venue` | Clearly fictional, non-private address and coordinates. | Avoids exposing a real meeting place. |

For the current local integration proof, these states are created per run by `OrganizerFlowTest`; migrations seed only taxonomy and local development sample data. A deployable demo must add an idempotent, documented reset/seed command before it can claim a public-demo fixture path. That requirement is carried by `SHOW-007`.

## Demo script

1. As a visitor, open the public events view and confirm `Release Journey Lab` is published and discoverable.
2. Sign in as `demo-confirmed`, RSVP `YES`, and open the generated ticket/QR code.
3. Sign in as `demo-waitlisted`, request RSVP, and confirm `WAITLISTED` with position `1`.
4. As `demo-confirmed`, cancel the RSVP. Sign in again as `demo-waitlisted` and confirm the RSVP is now `YES` with no waitlist position.
5. Open the promoted attendee’s ticket and record its code only in the local/session evidence—never in public documentation.
6. As `demo-organizer` (or a separately assigned scanner in a later fixture), open check-in and admit that ticket once. Rescan it and confirm the same check-in is returned rather than a second admission.
7. As `demo-organizer`, open the attendance report and confirm registered `1`, attended `1`, no-shows `0`, and attendance rate `100%`.

The demo is a failure if any authorization check, waitlist transition, duplicate-scanning guard, or reported count differs from the expected state. Record the failure; do not reset silently and present a later run as the original result.

## Supported walkthrough environment

| Area | Release target | Current evidence |
| --- | --- | --- |
| Desktop browser | Latest stable Chrome, Firefox, and Safari at 1280 px or wider. | Not yet manually verified. |
| Mobile browser | Latest stable Safari on iOS and Chrome on Android at 360 px or wider. | Not yet manually verified. |
| Assistive interaction | Keyboard-only primary path; screen-reader spot check after `SHOW-003`. | Not release evidence yet. |
| Scanner | Manual ticket-code entry is required; camera scanning is supplementary. | Camera/device matrix is not yet validated. |

The product must not claim support outside this matrix until corresponding evidence exists. Browser/device validation and accessibility evidence remain release gates, not documentation-only work.

## Explicitly out of release scope

- Payments, refunds, and financial reconciliation.
- Real-time social messaging or broad moderation workflows.
- Social-login provider authentication; it is intentionally unavailable.
- Production email verification/reset delivery until `SHOW-002` provides a real provider or removes the promise.
- File uploads or durable media storage; photos are URL metadata only.
- Provider-backed generative AI; current drafting, feedback, and recommendations are deterministic heuristics.
- Recurrence, feedback, photos, and recommendation quality as a condition of the primary demo journey.

## Automated and manual evidence

- **API/integration proof:** `apps/api/src/test/java/com/showup/api/OrganizerFlowTest.java`, `createPublishRsvpWaitlistTicketCheckInAndReport`. It creates isolated identities and fixtures, verifies capacity/waitlist promotion, ticket issuance, idempotent check-in, authorization, and report totals.
- **Manual smoke procedure:** [manual-smoke-test.md](manual-smoke-test.md). Run it against the candidate environment and retain dated evidence.
- **Release gate:** [release-evidence-checklist.md](../evidence/release-evidence-checklist.md). This contract does not itself release ShowUp.
