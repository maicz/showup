# ShowUp Master Audit & Remaining Work Backlog

This document organizes all audit findings and remaining engineering tasks by priority. It combines the architectural, security, database/performance, frontend usability, form validation, and accessibility audits.

---

## Workspace & Build Status

- **Backend (API)**: Java 25 / Spring Boot 4.1.0 — Architecture validated; unit, slice, and security tests configured.
- **Frontend (Web)**: Angular 22.1 / Vitest — Signal-based architecture, standalone components, 12 Vitest tests passing, production build passing.
- **Database**: PostgreSQL 18 + PostGIS — Schema migrations V1–V6 with Flyway.

---

## Priority 0 (P0) — Security, Privacy & Critical Runtime Blockers [COMPLETED ✅]

### 1. Fix Private Email & Identity Data Leak [DONE ✅]
- **Location**: [`MemberController.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/controller/MemberController.java#L46), [`MemberProfile.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/dto/MemberProfile.java)
- **Status**: Completed. `GET /api/members/{id}` returns sanitized `MemberSummary` without email or verification flags. Verified by tests.

### 2. Guard Online Meeting Links & Private Venue Details [DONE ✅]
- **Location**: [`EventService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/EventService.java#L120), [`EventDetail.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/dto/EventDetail.java#L14), [`event-detail.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-detail/event-detail.component.ts#L167)
- **Status**: Completed. `EventService.detail()` nullifies `onlineUrl` unless viewer is confirmed attendee (`RSVP == YES`), event host, or group organizer. UI displays locked banner when hidden. 5 unit tests passing.

### 3. Remove Unconditional Members API Call for Anonymous Visitors [DONE ✅]
- **Location**: [`group-detail.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/group/group-detail/group-detail.component.ts#L353-L373)
- **Status**: Completed. `group-detail.component.ts` only loads roster if `group.viewerStatus === 'ACTIVE'`, uses `group.memberCount` for tab header, and renders private roster placeholder for non-members.

### 4. Fix Aggressive 401 Global Logout & Redirect Interceptor [DONE ✅]
- **Location**: [`auth.interceptor.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/core/interceptors/auth.interceptor.ts#L25-L29)
- **Status**: Completed. Interceptor checks for active token before triggering logout/redirect. Tested with unit tests in `auth.interceptor.spec.ts`.

---

## Priority 1 (P1) — Database Performance & Scalability [COMPLETED ✅]

### 1. Eliminate N+1 Queries in Public Group Search [DONE ✅]
- **Location**: [`GroupRepository.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/repository/GroupRepository.java#L23), [`GroupMapper.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/mapper/GroupMapper.java#L24)
- **Status**: Completed. Attached `@EntityGraph(attributePaths = {"category"})` to `searchPublic` in `GroupRepository`, eliminating the extra SQL query per row when reading categories.

### 2. Fix N+1 Queries on Event Search Mapping [DONE ✅]
- **Location**: [`EventSearchRepositoryImpl.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/repository/EventSearchRepositoryImpl.java#L115), [`EventMapper.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/mapper/EventMapper.java#L16)
- **Status**: Completed. Re-architected native search to return page IDs, followed by a single JPQL query batch-fetching `group`, `group.category`, and `venue` associations with order preserved. Reduced database round-trips from $1 + 3N$ to 2 queries total.

### 3. Replace In-Memory Loop Queries in Group Activity Reporting [DONE ✅]
- **Location**: [`ReportingService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/ReportingService.java#L98-L108)
- **Status**: Completed. Replaced loop queries with dedicated JPQL aggregation queries (`RsvpRepository.aggregateRsvpsForGroupBetween` and `CheckInRepository.sumAdmittedCountForGroupBetween`), reducing query count from $1 + 2N$ to 4 constant queries. Tested with unit tests in `ReportingServiceTest`.

### 4. Add Trigram / Full-Text Indexing for Substring Searches [DONE ✅]
- **Location**: [`EventSearchRepositoryImpl.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/repository/EventSearchRepositoryImpl.java#L100), [`GroupRepository.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/repository/GroupRepository.java#L27), [`V7__add_trigram_indexes.sql`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/resources/db/migration/V7__add_trigram_indexes.sql)
- **Status**: Completed. Created Flyway migration V7 with `pg_trgm` extension and GIN trigram indexes on `lower(event.title)` and `lower(meetup_group.name)`. Updated queries in `EventSearchRepositoryImpl` and `GroupRepository` to use `lower(...) LIKE ...` matching the index expressions.

---

## Priority 2 (P2) — Frontend Form Validation & Usability

### 1. Replace Silent `[disabled]` Buttons with Inline Form Feedback
- **Location**:
  - [`login.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/auth/login/login.component.ts#L53)
  - [`register.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/auth/register/register.component.ts#L64)
  - [`reset-password.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/auth/reset-password/reset-password.component.ts#L50)
  - [`group-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/group/group-create/group-create.component.ts#L158)
  - [`event-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-create/event-create.component.ts#L247)
- **Issue**: Buttons are disabled silently when validation fails (e.g. password < 8 chars, invalid email, unselected category). Users receive no indication of what field is incomplete or invalid.
- **Fix**:
  - Add visual validation feedback (`is-invalid` CSS class on inputs, `<span class="form-error">` helper messages on blur/dirty states).
  - Clearly describe password requirements ("Minimum 8 characters") and required field indicators (`*`).

### 2. Bind Server-Side `fieldErrors` to Form Controls
- **Location**: All forms across `auth/`, `group/`, `event/`, and `profile/`.
- **Issue**: When backend Bean Validation fails (HTTP 400), Spring returns `fieldErrors: [{ field: "email", message: "must be a well-formed email address" }]`. The UI ignores these errors and displays only a generic toast message.
- **Fix**:
  - Replicate the `venueFieldErrors` pattern from `EventCreateComponent.saveNewVenue` across all forms to highlight invalid fields and render backend messages directly beneath the corresponding inputs.

### 3. Add Missing "Decline / Reject" Action in Organizer Console
- **Location**: [`group-manage.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/group/group-manage/group-manage.component.ts#L48), [`GroupService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/GroupService.java)
- **Issue**: The Pending Join Requests list only provides an "Approve Member" button. Organizers cannot decline or reject a join request.
- **Fix**:
  - Add a `declineMember(groupId, memberId)` service method and endpoint (`DELETE /api/groups/{id}/members/{memberId}` or status transition to `LEFT`/`REJECTED`).
  - Add a "Decline" button next to "Approve" with confirmation in `GroupManageComponent`.

### 4. Add Admission Fee Inputs to Event Creator
- **Location**: [`event-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-create/event-create.component.ts#L881)
- **Issue**: Fee is hardcoded to `0` / `'USD'`. Organizers cannot set admission ticket prices.
- **Fix**:
  - Add a "Ticket Price" input (amount + currency selector) in `EventCreateComponent`.

### 5. Fix Local Time Zone Drift in Event Date Picker
- **Location**: [`event-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-create/event-create.component.ts#L828)
- **Issue**: `<input type="datetime-local">` strings are parsed with `new Date(startsAtStr).toISOString()`, which applies the browser's local timezone offset instead of the event's selected `timeZone`.
- **Fix**:
  - Parse the datetime string in the context of the selected IANA `timeZone` before converting to UTC ISO string.

### 6. Add In-Flight Request Disabling & Destructive Confirmations
- **Location**:
  - [`group-detail.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/group/group-detail/group-detail.component.ts#L398): `leaveGroup()` has no confirmation dialog.
  - [`event-detail.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-detail/event-detail.component.ts#L979): `cancelRsvp()`, `deleteComment()`, `publishEvent()` lack in-flight loading guards.
  - [`staff-management.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/attendance/staff/staff-management.component.ts#L235): `removeStaff()` triggers immediately without confirmation.
- **Fix**:
  - Add confirmation dialogs before leaving groups or removing staff.
  - Add `loading` / `disabled` states to prevent double-click submissions.

### 7. Password Reset Token Single-Use Revocation
- **Location**: [`AuthService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/AuthService.java#L116-L123)
- **Issue**: Password reset tokens are stateless JWTs that remain valid for their full 2-hour TTL even after being used.
- **Fix**:
  - Invalidate reset tokens upon successful password update (e.g. by comparing token issue timestamp against `member.passwordUpdatedAt` or a version/nonce).

---

## Priority 3 (P3) — Accessibility, AI Copilot & Frontend Polish

### 1. Modal Dialog Accessibility & Keyboard Support
- **Location**: All modal overlays across `event-detail`, `group-detail`, and `event-create`.
- **Issue**: Modals do not trap focus, lack `aria-modal="true"`, and do not close on pressing `Escape`.
- **Fix**:
  - Add `@HostListener('document:keydown.escape')` to close active dialogs.
  - Ensure autofocus moves to the first interactive element upon opening and restores focus to the trigger button upon closing.

### 2. Screen Reader Live Announcements for Toasts
- **Location**: [`toast.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/shared/components/toast/toast.component.ts)
- **Issue**: Toast notifications lack `aria-live="polite"` and `role="status"`.
- **Fix**:
  - Wrap the toast container in an ARIA live region so dynamic status alerts are voiced by screen readers.

### 3. Implement Topic Filtering in AI Recommendations
- **Location**: [`CopilotService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/CopilotService.java#L157-L175)
- **Issue**: `getRecommendations(memberId)` queries followed topic IDs but takes `upcoming.limit(6)` without matching against `interestedTopicIds`.
- **Fix**:
  - Filter and rank upcoming events by matching group/event topics with the member's followed topic IDs.

### 4. Enable Angular Route Lazy Loading
- **Location**: [`app.routes.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/app.routes.ts)
- **Issue**: All components are eagerly imported, bundling the entire application into the initial script.
- **Fix**:
  - Convert route definitions to use `loadComponent: () => import('./path/to/component').then(m => m.ComponentClass)`.

### 5. Component Modularization
- **Location**: [`event-detail.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-detail/event-detail.component.ts) (1,100+ lines), [`event-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-create/event-create.component.ts) (900+ lines).
- **Issue**: Single-file components combining inline styles, large templates, and multi-feature logic.
- **Fix**:
  - Split out subcomponents (e.g. `EventTicketModalComponent`, `EventFeedbackListComponent`, `VenueCreateModalComponent`).

---

## Verification & Testing Commands

### Backend Suite
```bash
cd apps/api
./mvnw clean test
```

### Frontend Test & Production Build
```bash
cd apps/web
npm test -- --watch=false
npm run build
```
