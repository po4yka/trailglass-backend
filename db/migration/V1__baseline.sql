-- Baseline schema placeholder to validate Flyway wiring
CREATE TABLE IF NOT EXISTS service_health (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    note TEXT NOT NULL DEFAULT 'bootstrap'
);
