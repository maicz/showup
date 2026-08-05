# ShowUp

Event-planning app in the spirit of Meetup: event setup, capacity, registration, staffing,
QR check-in, attendance, and organizer reporting.

**Status:** The API is built — authentication, groups, venues, events, RSVPs with waitlisting,
ticketing, QR check-in, staffing, feedback, and organizer reporting all run end to end over
Postgres. The Angular client is still the original walking skeleton: it lists events and nothing
more.

## Docs

- [Domain model](docs/domain-model.md) — entities, DTOs, indexes, and the migration plan.
- [API reference](docs/api.md) — every endpoint, who may call it, and the auth model.
- [Spring AI getting started](docs/spring-ai-getting-started.md) — planned AI features, setup, and model choices.

## Stack

| Piece    | Version                  |
| -------- | ------------------------ |
| Java     | 25                       |
| Spring   | Boot 4.1.0 (Web MVC, Data JPA, Flyway, Validation, Security, OAuth2 Resource Server, Actuator) |
| Angular  | 22.1 (standalone, signals, SCSS) |
| Postgres | 18                       |
| Tests    | JUnit 5 + Testcontainers, Vitest |

## Layout

```
showup/
├── docker-compose.yml     # local Postgres
├── .env.example           # copy to .env to override DB settings
└── apps/
    ├── api/               # Spring Boot service
    │   └── src/main/
    │       ├── java/com/showup/api/        # packages by layer — see below
    │       └── resources/db/migration/     # Flyway migrations
    └── web/               # Angular client
        ├── proxy.conf.json                 # /api → localhost:8080
        └── src/app/features/event/         # event list component + service, one folder per feature area
```

Each app owns its own build (`pom.xml`, `package.json`) — there is no root-level build tool.

The API is organized **by layer**: each package holds one kind of thing, so the role of a class is
obvious from where it lives, and the allowed dependency direction is obvious from the diagram.
Every package carries a `package-info.java` stating what belongs in it.

```
com.showup.api
├── config/       Spring @Configuration — security chain, JWT beans, argument resolvers
├── controller/   REST endpoints; bind + validate, delegate, return DTOs
├── service/      transactional business logic and authorization rules
├── repository/   Spring Data JPA repositories
├── entity/       JPA entities and embeddables (Address, Money, BaseEntity)
├── dto/          request/response records — the wire format
├── enums/        enums shared by entities and DTOs (statuses, roles, formats)
├── mapper/       MapStruct entity ↔ DTO mappers
├── validation/   custom jakarta.validation constraints
├── exception/    domain exceptions, each mapped to one HTTP status
├── security/     bearer token → acting member id
└── util/         stateless helpers (JTS ↔ GeoPoint, RRULE expansion)
```

`controller → service → repository → entity`, with `mapper` translating entity to `dto` at the
edge. Nothing below the controller depends on anything above it. Controllers hold no business
logic: they bind, validate, and delegate. Services never mention `HttpStatus` — they throw the
exceptions in `exception/`, and `ApiExceptionHandler` is the single place a status code is
chosen.

Angular follows the same idea from the other direction: it is organized by feature
(`features/event/`), which is that framework's convention.

## Running it

Three terminals, in this order.

**1. Database**

```bash
cp .env.example .env      # optional; defaults work as-is
docker compose up -d
```

Postgres is published on host port **5433** (not 5432) so it can coexist with any other
local Postgres. Override with `POSTGRES_PORT` in `.env`.

**2. API** — http://localhost:8080

```bash
cd apps/api
./mvnw spring-boot:run
```

Flyway applies the migrations in `db/migration` on boot, building out the full domain model
(members, groups, venues, events, RSVPs, attendance — see [the domain model doc](docs/domain-model.md))
and seeding two demo events. Check it: `curl localhost:8080/api/events`.

**3. Web** — http://localhost:4200

```bash
cd apps/web
npm start
```

The dev server proxies `/api` to the API, so there is no CORS config to maintain.

## Tests

```bash
cd apps/api && ./mvnw test    # spins up a throwaway Postgres via Testcontainers
cd apps/web && npm test
```

API tests need Docker running but not `docker compose up` — Testcontainers provides its own
database. `TestApiApplication` does the same for `spring-boot:run` if you'd rather not manage
the compose stack while developing.

## Configuration

The API reads these environment variables (defaults in `application.yaml`):

| Variable      | Default                                  |
| ------------- | ---------------------------------------- |
| `DB_URL`      | `jdbc:postgresql://localhost:5433/showup` |
| `DB_USERNAME` | `showup`                                 |
| `DB_PASSWORD` | `showup`                                 |
| `SERVER_PORT` | `8080`                                   |
| `JWT_SECRET`  | a development-only key — **set this in any real environment** |
| `JWT_TTL`     | `PT12H` (ISO-8601 duration)              |

`JWT_SECRET` signs the access tokens with HS256, so it must be at least 32 bytes; the app refuses
to start if it is shorter, rather than failing at the first login.

Hibernate runs with `ddl-auto: validate` — the schema is owned by Flyway. Every schema change
is a new versioned migration, never an entity-driven auto-update.

## Next step

Build the Angular client against the API: sign-in, group and event browse, the RSVP button, and
the organizer's check-in screen. The endpoints those screens need all exist — see
[the API reference](docs/api.md).
