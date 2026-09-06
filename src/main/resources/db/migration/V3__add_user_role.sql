-- Roles were previously implicit: every stored user was a USER and the single
-- administrator existed only in configuration, so a second admin could not be
-- created. Existing rows keep that meaning via the default.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'));
