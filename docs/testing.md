# Testing ShowUp

## API integration suite

The API suite contains unit tests, Spring MVC slices, repository tests, and full HTTP integration flows. Full-context tests start their own disposable PostgreSQL/PostGIS container through Testcontainers; they never use the development Compose database.

### Prerequisites

- JDK 25, as declared in `apps/api/pom.xml`.
- A Docker-compatible runtime reachable through the `docker` CLI. Docker Desktop is the supported local option on macOS.
- Network access on the first run so Docker can pull `postgis/postgis:18-3.6` and Testcontainers' helper image.

Start Docker Desktop before testing, then verify it from the repository root:

```bash
./scripts/test-preflight.sh
```

Run the complete suite:

```bash
cd apps/api
../../scripts/test-preflight.sh
./mvnw test
```

The command is the integration gate. Do not replace it with `-Dtest=*Test`: that selection can hide suite configuration or integration failures. Surefire writes XML reports to `apps/api/target/surefire-reports/`; JaCoCo writes a local report to `apps/api/target/site/jacoco/`.

### Troubleshooting

- `Could not find a valid Docker environment`: start Docker Desktop and rerun the preflight script. Do not start the development Compose database as a workaround—the tests manage their own container.
- On Apple Silicon, the current PostGIS image may report `amd64` emulation. That is a performance warning, not a test exclusion; retain it in release evidence if it affects the observed duration.
- A failing test must remain a failure. Do not silently exclude it. Record any approved, time-bounded exception in the release-evidence checklist with owner, rationale, and expiration.

The verified clean-checkout result is recorded in [the baseline evidence](evidence/baselines/2026-09-19.md).
