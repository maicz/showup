# Release Evidence Checklist

Use one copy of this template for every ShowUp release candidate. A release is blocked until every required item has evidence, or an explicit exception is accepted below. Evidence must be a stable command output, CI run, screenshot/video, ADR, runbook, or externally verifiable URL—not an assertion in a ticket.

## Candidate record

| Field | Value |
| --- | --- |
| Candidate/version | |
| Commit/image digest | |
| Environment and date | |
| Owner/reviewer | |
| Evidence directory or links | |

## Required evidence

| Gate | Required evidence | Status | Link or note |
| --- | --- | --- | --- |
| Build | API and web production builds complete from the candidate commit. | ☐ | |
| Automated tests | API integration suite, web unit suite, browser smoke, and migration fresh-install/upgrade results. | ☐ | |
| Security | Dependency/image scan, authorization evidence, secret/config review, CORS/headers/rate-limit review. | ☐ | |
| Accessibility | Keyboard journey, screen-reader spot check, focus/contrast/reduced-motion and responsive reflow review. | ☐ | |
| Performance | Bundle and endpoint/concurrency measurements compared with stated budgets. | ☐ | |
| Data recovery | Backup artifact, restore runbook, and a successful restore exercise. | ☐ | |
| Operations | Image/stack health checks, structured logs/alerts, deploy and rollback proof. | ☐ | |
| Demo evidence | Seeded fixtures, public-network smoke result, annotated screenshots, and fallback recording. | ☐ | |
| Truthfulness | Public copy checked against actual deployment, provider, storage, and feature state. | ☐ | |

## Exceptions

An exception is valid only when every field is filled. Blank, open-ended, or self-approved exceptions block release.

| Missing gate | Risk and rationale | Mitigation | Approver | Expiry | Follow-up issue |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

## Sign-off

| Role | Name | Date | Decision |
| --- | --- | --- | --- |
| Engineering owner | | | |
| Reviewer | | | |
