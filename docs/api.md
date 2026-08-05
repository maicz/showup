# API reference

Every endpoint the service exposes, who may call it, and why. Shapes are in the
[domain model](domain-model.md#dtos); this document covers the surface and the rules.

## Authentication

Stateless bearer tokens. `POST /api/auth/register` and `/api/auth/login` return a
`TokenResponse`; send it back as `Authorization: Bearer <accessToken>`.

Tokens are HS256 JWTs signed with `JWT_SECRET`, issued and verified by Spring Security's
Nimbus-backed `JwtEncoder`/`JwtDecoder` — there is no hand-rolled JWT code. The `sub` claim is
the member id; `@CurrentMember` binds it straight to a controller parameter.

**Passwords** are BCrypt hashes in `member.password_hash`. That column never leaves the service:
no DTO and no mapper references it, which is the single strongest argument for
[entities never crossing the controller boundary](domain-model.md#dtos).

**SSO is not implemented.** `POST /api/auth/sso` returns **501**. Honouring it would mean
trusting the supplied `idToken`, which is only safe after verifying the provider's signature
against its JWKS along with the issuer, audience, and expiry. Accepting the token's `sub` without
that check is an authentication bypass — anyone could mint a token naming any subject. The
`member_identity` table and the lookup path are modelled and ready; the per-provider verification
is the missing piece.

## Authorization

Two independent layers, because they answer different questions.

| Layer | Question | Where |
| --- | --- | --- |
| Filter chain | Is there a valid token? | `config/SecurityConfig` |
| Service | Does this member's role in *this group* permit it? | `service/GroupAccessGuard` |

The second cannot live in the filter chain: the answer depends on which group the request
touches, which no URL pattern can express. Roles come from `group_membership.role`.

| Capability | Roles |
| --- | --- |
| Group settings, member roles, approvals | `ORGANIZER`, `CO_ORGANIZER` |
| Create/edit/publish/cancel events, venues, staffing, reports, attendee lists | the above plus `ASSISTANT_ORGANIZER`, `EVENT_ORGANIZER` |
| Scan tickets at the door | the above, **or** a `SCANNER` staff assignment for that event |
| RSVP, ticket, feedback | any authenticated member |

`SCANNER` is the one delegation outside the organizer roles: a volunteer can work the door for
one event without being given any standing rights over the group.

## Anonymous endpoints

Browsing is public — it is the first thing a signed-out visitor sees.

```
GET  /api/categories
GET  /api/topics?categorySlug=
GET  /api/events
GET  /api/events/search
GET  /api/events/{id}
GET  /api/events/{id}/feedback
GET  /api/groups
GET  /api/groups/{id}
GET  /api/groups/by-urlname/{urlname}
GET  /api/groups/{id}/events
```

`GET /api/events/{id}` is public but *richer* with a token: `viewerRsvp` is populated from it
when present, saving every client a round trip purely to decide the RSVP button's label. That is
why the matchers are single-segment — `/api/events/{id}` is public, `/api/events/{id}/attendees`
is very much not.

## Endpoints

### Auth

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/auth/register` | 201, returns a token |
| POST | `/api/auth/login` | 401 on bad credentials, with a message that does not reveal which half was wrong |
| POST | `/api/auth/sso` | 501, see above |

### Members

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/members/me` | |
| PUT | `/api/members/me` | |
| GET | `/api/members/{id}` | |
| GET/PUT | `/api/members/me/interests` | PUT replaces the whole set — idempotent, no add/remove ordering bugs |
| GET | `/api/members/me/groups` | |
| GET | `/api/members/me/events` | "my events": everything the caller is seated or waitlisted for |

### Groups

| Method | Path | Notes |
| --- | --- | --- |
| GET/POST | `/api/groups` | the creator becomes `ORGANIZER`, so a group is never left without one |
| GET/PUT | `/api/groups/{id}` | |
| POST | `/api/groups/{id}/members` | honours the join policy: `OPEN` → active, `APPROVAL_REQUIRED` → pending, `INVITE_ONLY` → 403 |
| DELETE | `/api/groups/{id}/members/me` | 422 if you are the last active organizer |
| GET | `/api/groups/{id}/members?status=` | members only — it is a roster |
| PUT | `/api/groups/{id}/members/{memberId}/role` | same last-organizer guard |
| POST | `/api/groups/{id}/members/{memberId}/approve` | |
| GET/POST | `/api/groups/{id}/venues` | |
| GET | `/api/venues/{id}` | |

### Events

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/groups/{groupId}/events` | created as `DRAFT` |
| PUT | `/api/events/{id}` | 422 once cancelled |
| POST | `/api/events/{id}/publish` | `DRAFT` → `PUBLISHED` only |
| POST | `/api/events/{id}/cancel` | records the reason and the timestamp |
| POST/DELETE | `/api/events/{id}/hosts` | an event must keep at least one host |
| GET | `/api/events/search` | facets below |

State transitions get their own endpoints rather than a patchable `status` field, so
`CANCELLED → DRAFT` is unrepresentable instead of merely discouraged.

`search` binds `lat`, `lon`, `radiusKm`, `categorySlug`, `topicSlugs`, `format`, `dateFrom`,
`dateTo`, `maxFee`, `availability`, `page`, `size`, `sort`. All optional. The radius filter is
PostGIS `ST_DWithin` against the venue's `geography` column. Only the *presence* of each clause is
dynamic — values are bound parameters and `sort` resolves through a fixed whitelist, so no caller
input reaches the SQL text.

### Event series

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/event-series` | the recurrence rule is parsed on create, so a bad rule fails now rather than later |
| GET | `/api/event-series/{id}` | |
| GET | `/api/groups/{groupId}/event-series` | |
| GET/POST | `/api/event-series/{id}/occurrences` | materializes `DRAFT` occurrence rows |

Occurrences are real `event` rows, not computed on read, so each can be individually cancelled,
re-venued, RSVPed to, and reported on.

The RRULE support is deliberately a subset: `FREQ` (DAILY/WEEKLY/MONTHLY/YEARLY), `INTERVAL`,
`COUNT`, `UNTIL`. Anything richer — `BYDAY`, `BYSETPOS`, `EXDATE` — is **rejected by name** rather
than ignored, because silently dropping a `BYDAY` produces a schedule that looks plausible and is
wrong. Expansion runs in the group's time zone, so a weekly 19:00 meetup stays at 19:00 across a
daylight-saving change.

### RSVPs

| Method | Path | Notes |
| --- | --- | --- |
| PUT | `/api/events/{id}/rsvp` | one RSVP per member per event, so re-answering replaces |
| GET | `/api/events/{id}/rsvp` | |
| DELETE | `/api/events/{id}/rsvp` | same transaction as answering `NO` |
| GET | `/api/events/{id}/attendees?status=` | organizer only |

Submitting `WAITLISTED` is rejected — placement is the server's decision. Send `YES`; you are
waitlisted if the event is full and the waitlist is on, and refused with 422 if it is off.

Seat allocation takes a `SELECT … FOR UPDATE` row lock on the event first. The decision to seat
or waitlist is a read-then-write on `yes_rsvp_count`; without the lock two concurrent RSVPs both
read the pre-increment value and the event oversells.

`yes_rsvp_count` counts **seats**, not rows — a member bringing two guests holds three of them —
because `capacity` is a headcount. Freeing seats walks the waitlist in position order and stops at
the first party too large to fit, rather than skipping past it: jumping the queue to fill a gap
generates support tickets.

### Attendance

| Method | Path | Notes |
| --- | --- | --- |
| POST/GET | `/api/events/{id}/ticket` | your own; issuing twice returns the same ticket rather than invalidating the first QR code |
| POST | `/api/events/{id}/check-ins` | organizer or `SCANNER` |
| POST | `/api/events/{id}/tickets/{ticketId}/revoke` | organizer |
| GET/POST | `/api/events/{id}/staff` | organizer; staff must belong to the group |
| DELETE | `/api/events/{id}/staff/{assignmentId}` | organizer |

Check-in is idempotent by construction: `check_in.ticket_id` is unique, so a second scan of the
same code returns the first check-in instead of admitting another head. Double-scanning is a fact
of life at a door.

### Feedback and reporting

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/events/{id}/feedback` | attendees only, after the event has started; one per member |
| GET | `/api/events/{id}/feedback` | public |
| GET | `/api/events/{id}/reports/attendance` | organizer |
| GET | `/api/groups/{id}/reports/activity?from=&to=` | organizer |

Submitting feedback recomputes the group's rating from every row rather than nudging a running
average, so the rollup cannot drift away from the data.

Attendance counts seats on both sides — registered and admitted — so a party of three whose guests
stayed home reports as two no-shows, not one.

## Errors

Every failure returns the same `ApiError` body, including the ones raised in the security filter
chain before any controller advice could see them.

| Status | Meaning |
| --- | --- |
| 400 | malformed body, or bean validation failed — `fieldErrors` names each one |
| 401 | missing, expired, or invalid token; bad credentials |
| 403 | authenticated, but your group role does not permit this |
| 404 | no such resource |
| 409 | collides with existing state — duplicate email, urlname, membership, or a lost optimistic lock |
| 422 | the body was fine; the domain rule said no — RSVPs closed, event cancelled, waitlist off |
| 501 | modelled but deliberately not built (SSO) |

The 400/422 split is the useful one: 400 means *fix your request*, 422 means *the request was
fine, the state was not*. Retrying a 400 unchanged is pointless; retrying a 422 later may work.
