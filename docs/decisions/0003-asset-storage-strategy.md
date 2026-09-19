# ADR-0003: Use object storage for production media, not database blobs

- Status: Accepted
- Date: 2026-09-19
- Owners: Mihai
- Related issues: SHOW-002

## Context

The current photo feature stores an externally hosted URL and metadata. It does not accept files or own the lifecycle, validation, retention, or availability of media assets.

## Decision

Production media will use object storage with narrowly scoped, time-limited upload URLs. The API will persist metadata and object references, never image blobs. If that storage path is not implemented for the public demo, photo uploads will be disabled rather than represented as a complete upload capability.

## Consequences

- SHOW-002 must validate permitted source/upload URLs and define authorization, size/type limits, and retention.
- Production configuration will hold storage credentials externally; none belong in the image or repository.
- Existing external-URL handling remains a local-development capability only.
