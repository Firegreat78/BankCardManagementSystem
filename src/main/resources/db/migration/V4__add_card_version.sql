-- Optimistic locking for the write paths that do not take a row lock (admin
-- update, status transitions): a stale write is rejected instead of silently
-- overwriting a concurrent change. Transfers additionally take a row lock,
-- because they must read a balance before writing it.
ALTER TABLE card ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
