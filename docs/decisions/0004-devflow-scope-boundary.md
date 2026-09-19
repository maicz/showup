# ADR-0004: Constrain DevFlow to an auditable delivery trace

- Status: Accepted
- Date: 2026-09-19
- Owners: Mihai
- Related issues: DEV-001 through DEV-011

## Context

DevFlow must demonstrate a second, distinct proof product without becoming an undifferentiated issue-tracker clone or delaying ShowUp's release.

## Decision

DevFlow's first releasable journey is: create a goal, record a structured decision, deliver an issue linked to both, associate a validated pull-request reference, and record a release with an auditable timeline. Boards, real-time collaboration, broad notifications, and deep GitHub synchronisation are explicitly out of scope for the initial slice. No DevFlow implementation begins until SHOW-010 is complete.

## Consequences

- DEV-001 validates this workflow before application code exists.
- The first PR linkage is a validated manual reference; a read-only GitHub integration requires later evidence that it earns its operating cost.
- The scope boundary is a release-quality constraint, not a future-feature promise.
