# ShowUp

Event-planning platform in the spirit of Meetup: community group management, event discovery, waitlisted RSVPs, admission ticketing with deterministic QR codes, live door check-in scanner, staff delegation, post-event feedback, attendance analytics, and copilot recommendations.

---

## Architecture & Tech Stack

| Tier | Technology | Key Libraries & Capabilities |
| --- | --- | --- |
| **Backend API** | Java 25, Spring Boot 4.1.0 | Spring Security (Stateless Nimbus JWT), Spring Data JPA, Hibernate, Flyway, MapStruct, JTS Spatial Geometry, Spring AI Copilot |
| **Frontend Web** | Angular 22.1 | Standalone Components, Signals (`signal`, `computed`), Built-in Control Flow (`@if`, `@for`), Standalone SVG QR Engine, SCSS Design System |
| **Database** | PostgreSQL 18 + PostGIS | Full relational integrity, check constraints, GIST spatial index, JSONB aggregates |
| **Testing** | JUnit 5, Mockito, WebMvcTest, Vitest | Fast isolated service/controller slice tests (47 Java tests) & reactive frontend component tests (11 Vitest tests) |

---

## Directory Structure

```
showup/
├── docker-compose.yml              # Local PostgreSQL container (port 5433)
├── .env.example                    # DB configuration template
├── docs/                           # Architectural guides & API specs
│   ├── api.md                      # Comprehensive REST endpoint reference
│   ├── domain-model.md             # Complete entity, DTO, and DB schema reference
│   └── spring-ai-getting-started.md# AI copilot & synthesis design
└── apps/
    ├── api/                        # Spring Boot 4.1 backend application
    │   ├── src/main/java/com/showup/api/
    │   │   ├── config/             # SecurityConfig, JwtConfig, WebMvcConfig
    │   │   ├── controller/         # REST Controllers (Auth, Members, Groups, Events, Comments, Photos, CheckIns, Staff, AI, Reports)
    │   │   ├── dto/                # Request / Response records (Wire format)
    │   │   ├── entity/             # JPA Entities & Value Objects (Address, Money)
    │   │   ├── enums/              # Statuses, roles, formats, join policies
    │   │   ├── exception/          # Domain exceptions & Global ApiExceptionHandler
    │   │   ├── mapper/             # MapStruct entity ↔ DTO converters
    │   │   ├── repository/         # Spring Data JPA repositories with spatial queries
    │   │   ├── security/           # @CurrentMember resolver, SecurityContext helpers
    │   │   ├── service/            # Transactional business logic & access guards
    │   │   └── util/               # RRULE recurrence expansion, JTS Geometry helpers
    │   └── src/main/resources/db/migration/ # Flyway SQL migrations (V1 - V5)
    └── web/                        # Angular 22.1 web application
        ├── src/app/
        │   ├── core/
        │   │   ├── guards/         # authGuard, noAuthGuard
        │   │   ├── interceptors/   # authInterceptor (Bearer token injection, 401 handling)
        │   │   ├── models/         # TypeScript interfaces matching API DTOs
        │   │   └── services/       # Core Angular services with reactive Signals
        │   ├── features/
        │   │   ├── attendance/     # Live Door Check-In Scanner & Staff Volunteer Roster
        │   │   ├── auth/           # Login, Register, Forgot Password, Reset Password, Verify Email
        │   │   ├── dashboard/      # Member Dashboard, RSVPs, Ticket Pass Launcher, AI Recommendations
        │   │   ├── event/          # Faceted Event Discovery, Rich Event Details, AI Copilot Event Creator
        │   │   ├── group/          # Group Directory, Community Detail, Group Creator, Organizer Console
        │   │   ├── profile/        # Member Profile Editor & Followed Interests Taxonomy Selector
        │   │   └── report/         # Post-Event Attendance Analytics & Group Activity Trends
        │   └── shared/             # Header, Footer, Toast Alerts, SVG QrCodeComponent
        └── proxy.conf.json         # Dev proxy `/api` → `http://localhost:8080`
```

---

## Features Implemented

### 1. Authentication & Member Lifecycle
- Stateless HS256 JWT auth with `@CurrentMember` binding.
- Register & Login with BCrypt password hashing.
- SSO identity linking is modelled but intentionally disabled until real Google/Apple/Facebook token verification is configured.
- Self-service Forgot Password & Reset Password with signed action tokens.
- Email verification lifecycle and resend token workflow.
- Member profile management (name, bio, home city, avatar).
- Interactive interest taxonomy selection from 24 curated categories.

### 2. Community Groups & Access Control
- Group discovery, category filtering, search, and member counts.
- Join policies: `OPEN` (instant membership), `APPROVAL_REQUIRED` (organizer approval queue), `INVITE_ONLY`.
- Organizer console with role management (`ORGANIZER`, `CO_ORGANIZER`, `ASSISTANT_ORGANIZER`, `EVENT_ORGANIZER`, `MEMBER`).
- Venue directory management per group.

### 3. Events & Recurring Series
- Event creation with single event or recurring series via iCalendar RRULE recurrence rules.
- Faceted search (keyword, format: `IN_PERSON` / `ONLINE` / `HYBRID`, category, followed topics, availability: `SEATS_AVAILABLE` / `WAITLIST`).
- Host assignment, cancellation with notifications, and publication workflow.
- Threaded discussions & comments with single-level replies and soft-delete redactions.
- Event photo gallery with upload and moderation controls.
- Post-event attendee reviews and 5-star ratings.

### 4. RSVPs, Ticketing & Door Check-In
- Capacity enforcement with automatic waitlisting and position tracking.
- Per-RSVP guest limits.
- Admission ticket generation with scannable SVG QR codes.
- Live organizer Door Check-in & Scanner screen:
  - Instant camera/barcode scanner & manual ticket code search.
  - Idempotency protection with warnings for duplicate check-ins.
  - Real-time turnout stats and attendee search roster.
- Volunteer staff delegation (`SCANNER`, `GREETER`, `SETUP`, `AV`, `CLEANUP`).

### 5. AI Copilot & Insights
- **AI Event Drafter**: Generates title, description, format, and capacity from natural language notes.
- **AI Feedback Synthesis**: Analyzes attendee reviews to extract overall sentiment, narrative summary, and top themes.
- **AI Recommendation Engine**: Recommends personalized upcoming events based on followed member interest topics.

### 6. Analytics & Reporting
- Event attendance report: registered count, attended count, no-shows, turnout percentage, staff scan totals, and check-in timeline.
- Group activity report: total events organized, turnout rate, new member growth, and satisfaction ratings.

---

## Quick Start

### 1. Start PostgreSQL
```bash
docker compose up -d
```
*PostgreSQL will be running on port 5433 with database `showup`.*

### 2. Start Backend API
```bash
cd apps/api
./mvnw spring-boot:run
```
*The API starts at `http://localhost:8080`. Flyway automatically runs database migrations and seeds initial taxonomy.*

### 3. Start Frontend Web Client
```bash
cd apps/web
npm install
npm start
```
*Open `http://localhost:4200` to access the application.*

---

## Running Tests

### Backend Unit & Controller Tests
```bash
cd apps/api
./mvnw test -Dtest=*Test
```
*Executes all 47 unit, mockito, and WebMvc slice tests (0 failures, 0 errors).*

### Frontend Vitest Tests
```bash
cd apps/web
npm test -- --watch=false
```
*Executes all 11 component and service unit tests (0 failures, 0 errors).*
