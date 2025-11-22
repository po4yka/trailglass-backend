-- Photo storage tables
-- Note: This is separate from photo_attachments in V2, which is for attaching photos to memories/journals.
-- The photos table is for direct photo uploads and management with storage backends (S3/Postgres).

CREATE TABLE IF NOT EXISTS photos (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    file_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key TEXT NOT NULL,
    storage_backend TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    blob_deleted_at TIMESTAMPTZ,
    server_version BIGSERIAL NOT NULL
);

CREATE INDEX IF NOT EXISTS photos_user_idx ON photos(user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS photo_blobs (
    storage_key TEXT PRIMARY KEY,
    content_type TEXT,
    data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
