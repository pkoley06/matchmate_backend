-- Milestone 11: self-service "Disable account" (soft, reversible) distinct from deletion.
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS disabled_at TIMESTAMP WITH TIME ZONE;

-- Accounts deactivated through DELETE /users/me before this migration are deletions.
UPDATE app_users SET account_status = 'DELETED' WHERE is_active = false AND account_status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_app_users_account_status ON app_users(account_status);
