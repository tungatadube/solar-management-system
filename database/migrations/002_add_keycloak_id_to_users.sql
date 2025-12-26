-- Migration: Add Keycloak integration columns to users table
-- Date: 2025-12-26
-- Description: Adds keycloak_id column for user synchronization from Keycloak JWT tokens
--              Makes password nullable since authentication is handled by Keycloak
--              Adds sync metadata columns to track synchronization source and timing

-- Add keycloak_id column (unique identifier from JWT 'sub' claim)
ALTER TABLE users ADD COLUMN IF NOT EXISTS keycloak_id VARCHAR(255);

-- Create unique index for performance (partial index to allow NULLs)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_keycloak_id ON users(keycloak_id)
WHERE keycloak_id IS NOT NULL;

-- Make password nullable (unused for Keycloak authentication)
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Add sync metadata columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_keycloak_sync TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS sync_source VARCHAR(50) DEFAULT 'MANUAL';

-- Add comments for documentation
COMMENT ON COLUMN users.keycloak_id IS 'Keycloak user UUID from JWT sub claim - primary identifier for Keycloak users';
COMMENT ON COLUMN users.last_keycloak_sync IS 'Timestamp of last synchronization from Keycloak JWT';
COMMENT ON COLUMN users.sync_source IS 'Source of user record: KEYCLOAK_AUTO (auto-synced from JWT), KEYCLOAK_ADMIN (admin API), or MANUAL (manually created)';
COMMENT ON COLUMN users.password IS 'Password field (nullable) - unused for Keycloak authentication, set to random UUID for Keycloak users';
