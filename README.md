# ShowUp

Event-planning app in the spirit of Meetup: event setup, capacity, registration, staffing,
QR check-in, attendance, and organizer reporting.

**Status:** Walking skeleton. A single vertical slice (list events) runs end to end —
Postgres → Flyway → JPA → REST → Angular. None of the product features above are built yet.

## Stack

| Piece    | Version                  |
| -------- | ------------------------ |
| Java     | 25                       |
| Spring   | Boot 4.1.0 (Web MVC, Data JPA, Flyway, Validation, Actuator) |
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
    │       ├── java/com/showup/api/event/   # Event slice: entity, repo, controller
    │       └── resources/db/migration/      # Flyway migrations
    └── web/               # Angular client
        ├── proxy.conf.json                  # /api → localhost:8080
        └── src/app/event/                   # event list component + service
```

Each app owns its own build (`pom.xml`, `package.json`) — there is no root-level build tool.

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

Flyway applies `V1__create_event.sql` on boot, which creates the `event` table and seeds two
rows. Check it: `curl localhost:8080/api/events`.

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

Hibernate runs with `ddl-auto: validate` — the schema is owned by Flyway. Every schema change
is a new versioned migration, never an entity-driven auto-update.

## Next step

Define one end-to-end organizer workflow (create event → register → check in) and build it as
the next vertical slice.
