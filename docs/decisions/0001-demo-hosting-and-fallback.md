# ADR-0001: Keep the public demo independent and retain a recorded fallback

- Status: Accepted
- Date: 2026-09-19
- Owners: Mihai
- Related issues: SHOW-001, SHOW-010, INFRA-001

## Context

The portfolio must remain useful when an interactive demo is unavailable. A home-lab outage, maintenance window, or provider incident must not erase the evidence of the product journey.

## Decision

The public ShowUp demo will be hosted independently of the portfolio site on a managed, container-capable host selected in INFRA-001. The portfolio will link to both the live demo and a versioned, labelled walkthrough recording stored independently of that host. A home lab may demonstrate operations, but public portfolio availability must not depend on it.

## Consequences

- SHOW-010 must prove the public path and capture the matching fallback walkthrough.
- INFRA-001 selects the specific host, domains, recovery objectives, and ingress design without changing this independence boundary.
- A live demo is never the only case-study evidence.
