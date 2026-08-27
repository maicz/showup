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

**SSO and Account Lifecycle:**
- `POST /api/auth/sso` returns `501 Not Implemented` until provider-side token verification is configured; unverified tokens are never trusted.
- `POST /api/auth/forgot-password` and `POST /api/auth/reset-password` handle signed reset action tokens.
- `POST /api/auth/verify-email` and `POST /api/auth/resend-verification` verify member emails.

The current `NotificationService` is a safe local adapter: it records a masked recipient and
non-reversible token fingerprint, but does not deliver email. Wire an SMTP/provider adapter before
presenting verification and reset emails as delivered in a deployed environment.

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
| RSVP, ticket, feedback, comment, photo | any authenticated member |

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
GET  /api/events/{id}/comments
GET  /api/events/{id}/photos
GET  /api/groups
GET  /api/groups/{id}
GET  /api/groups/by-urlname/{urlname}
GET  /api/groups/{id}/events
```

`GET /api/events/{id}` is public but *richer* with a token: `viewerRsvp` is populated from it
when present, saving every client a round trip purely to decide the RSVP button's label.

## Endpoints

### Auth

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/auth/register` | 201, returns a token and queues verification through the configured notification adapter |
| POST | `/api/auth/login` | 401 on bad credentials |
| POST | `/api/auth/sso` | 501 until real provider-token verification is configured |
| POST | `/api/auth/forgot-password` | Queues a password reset through the configured notification adapter |
| POST | `/api/auth/reset-password` | Resets password with action token |
| POST | `/api/auth/verify-email` | Confirms email verification token |
| POST | `/api/auth/resend-verification` | Resends verification token to current member |

### Members

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/members/me` | Current authenticated profile |
| PUT | `/api/members/me` | Updates profile name, bio, city, avatar |
| GET | `/api/members/{id}` | Public member summary |
| GET/PUT | `/api/members/me/interests` | PUT replaces followed topic set |
| GET | `/api/members/me/groups` | Groups the member has joined |
| GET | `/api/members/me/events` | "My events," enriched with `rsvpStatus`, guest count, and waitlist position |

### Groups

| Method | Path | Notes |
| --- | --- | --- |
| GET/POST | `/api/groups` | Creator becomes `ORGANIZER` |
| GET/PUT | `/api/groups/{id}` | |
| POST | `/api/groups/{id}/members` | Honours join policy (`OPEN`, `APPROVAL_REQUIRED`, `INVITE_ONLY`) |
| DELETE | `/api/groups/{id}/members/me` | Leave group (guarded if last organizer) |
| GET | `/api/groups/{id}/members?status=` | Member roster |
| PUT | `/api/groups/{id}/members/{memberId}/role` | Updates member role |
| POST | `/api/groups/{id}/members/{memberId}/approve` | Approves pending membership |
| GET/POST | `/api/groups/{id}/venues` | Group venue catalog |
| GET | `/api/venues/{id}` | |

### Events

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/groups/{groupId}/events` | Created as `DRAFT` |
| PUT | `/api/events/{id}` | Updates event details |
| POST | `/api/events/{id}/publish` | `DRAFT` → `PUBLISHED` |
| POST | `/api/events/{id}/cancel` | Records reason and timestamp |
| POST/DELETE | `/api/events/{id}/hosts` | Event host assignments |
| GET | `/api/events/search` | Faceted keyword/group, geo, date, price, format, availability, sort, and paged search |

### Comments & Media

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/events/{id}/comments` | Lists comments ordered by date (soft-deleted redacted) |
| POST | `/api/events/{id}/comments` | Adds comment or threaded reply |
| DELETE | `/api/events/{id}/comments/{commentId}` | Soft-deletes comment (author or organizer) |
| GET | `/api/events/{id}/photos` | Lists uploaded event photos |
| POST | `/api/events/{id}/photos` | Uploads photo metadata |
| DELETE | `/api/events/{id}/photos/{photoId}` | Deletes photo (uploader or organizer) |

### Event series

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/event-series` | Creates recurring series with RRULE rule |
| GET | `/api/event-series/{id}` | |
| GET | `/api/groups/{groupId}/event-series` | |
| GET/POST | `/api/event-series/{id}/occurrences` | Materializes draft occurrence rows |

### RSVPs

| Method | Path | Notes |
| --- | --- | --- |
| PUT | `/api/events/{id}/rsvp` | Submit RSVP with guest count (`YES` / `NO`) |
| GET | `/api/events/{id}/rsvp` | Viewer RSVP status |
| DELETE | `/api/events/{id}/rsvp` | Cancels RSVP |
| GET | `/api/events/{id}/attendees?status=` | Organizer attendee roster |

### Attendance & Staffing

| Method | Path | Notes |
| --- | --- | --- |
| POST/GET | `/api/events/{id}/ticket` | Generates / fetches QR admission ticket |
| POST | `/api/events/{id}/check-ins` | Idempotent QR scan / manual check-in |
| POST | `/api/events/{id}/tickets/{ticketId}/revoke` | Revokes admission ticket |
| GET/POST | `/api/events/{id}/staff` | Assigns volunteer roles (`GREETER`, `SCANNER`, etc.) |
| DELETE | `/api/events/{id}/staff/{assignmentId}` | Removes staff assignment |

### Feedback & Reporting

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/events/{id}/feedback` | Star rating & review from attendees |
| GET | `/api/events/{id}/feedback` | Public feedback list |
| GET | `/api/events/{id}/reports/attendance` | Registered, attended, no-show, scan timeline |
| GET | `/api/groups/{id}/reports/activity?from=&to=` | Group activity & turnout analytics |

### AI Copilot & Discovery

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/ai/copilot/draft-event` | Generates title, description, format, topics from prompt |
| GET | `/api/ai/feedback/{eventId}/summary` | Synthesizes attendee sentiment, themes, highlights |
| GET | `/api/ai/recommendations` | Personalized event recommendations for member |
