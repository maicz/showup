-- PostgreSQL pg_trgm extension and GIN trigram indexes for fast case-insensitive substring search

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_event_title_trgm ON event USING gin (lower(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_group_name_trgm ON meetup_group USING gin (lower(name) gin_trgm_ops);
