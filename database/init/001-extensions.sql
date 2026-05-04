-- Runs once, on first boot of an empty data volume.
-- See database/README.md for semantics.

-- pgcrypto: gen_random_uuid() for generated UUID defaults, digest() for hashing.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- pg_stat_statements: query-level performance telemetry for future tuning.
-- Requires shared_preload_libraries=pg_stat_statements in postgresql.conf.
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
