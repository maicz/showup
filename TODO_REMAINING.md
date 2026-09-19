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

## Priority 2 (P2) — Frontend Form Validation & Usability [COMPLETED ✅]

### 1. Replace Silent `[disabled]` Buttons with Inline Form Feedback [DONE ✅]
- **Location**:
  - [`login.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/auth/login/login.component.ts)
  - [`register.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/auth/register/register.component.ts)
  - [`reset-password.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/auth/reset-password/reset-password.component.ts)
  - [`forgot-password.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/auth/forgot-password/forgot-password.component.ts)
  - [`group-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/group/group-create/group-create.component.ts)
  - [`event-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-create/event-create.component.ts)
  - [`profile.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/profile/profile.component.ts)
- **Status**: Completed. Removed silent button disabling; added visual indicators (`*`), `.is-invalid` highlighting, and `<span class="form-error">` inline error messages with active validation upon submit and dynamic clearing upon input.

### 2. Bind Server-Side `fieldErrors` to Form Controls [DONE ✅]
- **Location**: All forms across `auth/`, `group/`, `event/`, and `profile/`.
- **Status**: Completed. Extended all submission error handlers to inspect `err?.error?.fieldErrors`, mapping backend Bean Validation violations directly to form fields for inline display.

### 3. Add Missing "Decline / Reject" Action in Organizer Console [DONE ✅]
- **Location**: [`group-manage.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/group/group-manage/group-manage.component.ts), [`GroupService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/GroupService.java), [`GroupController.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/controller/GroupController.java), [`group.service.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/core/services/group.service.ts)
- **Status**: Completed. Added `POST /api/groups/{id}/members/{memberId}/decline` endpoint transitioning `PENDING_APPROVAL` status to `LEFT`. Added frontend service method and Decline button with confirmation dialog and list refreshment.

### 4. Add Admission Fee Inputs to Event Creator [DONE ✅]
- **Location**: [`event-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-create/event-create.component.ts)
- **Status**: Completed. Added Ticket Price (`feeAmount`) and Currency (`feeCurrency`) form controls to the event creation template, converting amount to minor units (`feeAmountMinor`) in the create event request.

### 5. Fix Local Time Zone Drift in Event Date Picker [DONE ✅]
- **Location**: [`date.utils.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/core/utils/date.utils.ts), [`event-create.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-create/event-create.component.ts)
- **Status**: Completed. Created `localDateTimeToUtcIso` utility with full unit test coverage using `Intl.DateTimeFormat` to resolve local timezone offsets relative to the event's selected IANA timezone, preventing browser timezone drift.

### 6. Add In-Flight Request Disabling & Destructive Confirmations [DONE ✅]
- **Location**:
  - [`group-detail.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/group/group-detail/group-detail.component.ts): Added confirmation dialog and `leavingGroup` in-flight signal and disabled state.
  - [`event-detail.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/event/event-detail/event-detail.component.ts): Added confirmation dialogs and in-flight guards (`cancellingRsvp`, `deletingCommentId`, `publishingEvent`).
  - [`staff-management.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/features/attendance/staff/staff-management.component.ts): Added confirmation dialog and `removingStaffId` disabled state.
- **Status**: Completed. Attached confirmation dialogs and in-flight guard states across GroupDetail (`leavingGroup`), StaffManagement (`removingStaffId`), and EventDetail (`cancellingRsvp`, `deletingCommentId`, `publishingEvent`) to prevent duplicate submissions.

### 7. Password Reset Token Single-Use Revocation [DONE ✅]
- **Location**: [`AuthService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/AuthService.java), [`Member.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/entity/Member.java), [`V8__add_password_updated_at_to_member.sql`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/resources/db/migration/V8__add_password_updated_at_to_member.sql)
- **Status**: Completed. Created Flyway migration V8 adding `password_updated_at` column to `member`. Enforced in `AuthService.decodeActionToken` and `resetPassword` by rejecting any token issued before `member.passwordUpdatedAt`. Tested and verified with unit tests.

---

## Priority 3 (P3) — Accessibility, AI Copilot & Frontend Polish

### 1. Modal Dialog Accessibility & Keyboard Support [DONE ✅]
- **Location**: All modal overlays across `event-detail`, `group-detail`, and `event-create`.
- **Issue**: Modals do not trap focus, lack `aria-modal="true"`, and do not close on pressing `Escape`.
- **Fix**:
  - Add `@HostListener('document:keydown.escape')` to close active dialogs.
  - Ensure autofocus moves to the first interactive element upon opening and restores focus to the trigger button upon closing.
- **Status**: Completed. All event, group, and venue dialogs use a reusable focus trap, close with Escape, and restore focus after dismissal.

### 2. Screen Reader Live Announcements for Toasts [DONE ✅]
- **Location**: [`toast.component.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/shared/components/toast/toast.component.ts)
- **Issue**: Toast notifications lack `aria-live="polite"` and `role="status"`.
- **Fix**:
  - Wrap the toast container in an ARIA live region so dynamic status alerts are voiced by screen readers.
- **Status**: Completed. Toasts use a polite ARIA live region, `role="status"`, and accessible dismissal labels.

### 3. Implement Topic Filtering in AI Recommendations [DONE ✅]
- **Location**: [`CopilotService.java`](file:///Users/mihaiz/dev/projects/personal/showup/apps/api/src/main/java/com/showup/api/service/CopilotService.java#L157-L175)
- **Issue**: `getRecommendations(memberId)` queries followed topic IDs but takes `upcoming.limit(6)` without matching against `interestedTopicIds`.
- **Fix**:
  - Filter and rank upcoming events by matching group/event topics with the member's followed topic IDs.
- **Status**: Completed. Recommendations filter published events to followed group topics using one bulk group-topic lookup, with service coverage.

### 4. Enable Angular Route Lazy Loading [DONE ✅]
- **Location**: [`app.routes.ts`](file:///Users/mihaiz/dev/projects/personal/showup/apps/web/src/app/app.routes.ts)
- **Issue**: All components are eagerly imported, bundling the entire application into the initial script.
- **Fix**:
  - Convert route definitions to use `loadComponent: () => import('./path/to/component').then(m => m.ComponentClass)`.
- **Status**: Completed. All feature routes now load components on demand.

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
