# ADR-0002: Do not present log-only notifications as delivered email

- Status: Accepted
- Date: 2026-09-19
- Owners: Mihai
- Related issues: SHOW-002

## Context

The current notification service logs masked verification and password-reset delivery events. It has no transactional-email provider adapter, delivery status, bounce handling, or production credentials.

## Decision

Until a transactional provider is selected and a real adapter is tested, the public demo must hide or clearly label flows that promise email delivery. Production implementation will use a provider-backed adapter configured only through external secrets, with non-production recipient safety controls and delivery-failure observability.

## Consequences

- Registration, verification, and password-reset copy must remain truthful in the demo.
- SHOW-002 owns provider selection, adapter implementation, enumeration review, and delivery tests.
- Logging a token fingerprint remains diagnostic-only and is not delivery evidence.
