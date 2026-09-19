#!/usr/bin/env sh
# Verify the only external dependency for the Testcontainers-backed API suite.
set -eu

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI was not found. Install Docker Desktop or another Docker-compatible runtime." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not reachable. Start Docker Desktop (or the configured container runtime), then retry." >&2
  exit 1
fi

docker version --format 'Docker server {{.Server.Version}} is ready for Testcontainers.'
