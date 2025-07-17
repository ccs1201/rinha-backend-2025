#!/bin/bash
set -e

echo "Applying full custom Postgres configuration..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    ALTER SYSTEM SET max_connections = 100;
    ALTER SYSTEM SET shared_buffers = '16MB';
    ALTER SYSTEM SET effective_cache_size = '64MB';
    ALTER SYSTEM SET work_mem = '1MB';
    ALTER SYSTEM SET maintenance_work_mem = '8MB';
    ALTER SYSTEM SET wal_buffers = '512kB';
    ALTER SYSTEM SET effective_io_concurrency = 1;
    ALTER SYSTEM SET random_page_cost = 1;
    ALTER SYSTEM SET seq_page_cost = 1;
    ALTER SYSTEM SET checkpoint_timeout = '2min';
    ALTER SYSTEM SET checkpoint_completion_target = 0.9;
    ALTER SYSTEM SET synchronous_commit = off;
    ALTER SYSTEM SET fsync = off;
    ALTER SYSTEM SET full_page_writes = off;
    ALTER SYSTEM SET log_min_duration_statement = '1000ms';
    ALTER SYSTEM SET log_statement = 'none';
    ALTER SYSTEM SET log_duration = off;
    ALTER SYSTEM SET log_lock_waits = on;
    ALTER SYSTEM SET log_error_verbosity = 'terse';
    ALTER SYSTEM SET log_min_messages = 'info';
    ALTER SYSTEM SET log_min_error_statement = 'info';
    ALTER SYSTEM SET pg_trgm.similarity_threshold = 0.08;
EOSQL

echo "Reloading PostgreSQL config..."
pg_ctl reload

echo "Full custom configuration applied!"
