-- Sync service tables for generic data synchronization
-- Note: This is separate from the sync_versions table in V2, which tracks per-entity sync state.
-- These tables provide a more flexible sync mechanism for arbitrary data payloads.

CREATE TABLE IF NOT EXISTS sync_version_counters (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    current_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sync_data (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    payload TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    server_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS sync_data_user_version_idx ON sync_data(user_id, server_version);

CREATE TABLE IF NOT EXISTS encrypted_sync_data (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    payload TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    server_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS encrypted_sync_data_user_version_idx ON encrypted_sync_data(user_id, server_version);

CREATE TABLE IF NOT EXISTS sync_conflicts (
    id UUID PRIMARY KEY,
    entity_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    server_version BIGINT NOT NULL,
    device_version BIGINT NOT NULL,
    server_payload TEXT NOT NULL,
    device_payload TEXT NOT NULL,
    is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS sync_conflicts_user_idx ON sync_conflicts(user_id, resolved_at);

CREATE TABLE IF NOT EXISTS device_sync_state (
    device_id UUID PRIMARY KEY REFERENCES devices(id),
    user_id UUID NOT NULL REFERENCES users(id),
    last_sync_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
