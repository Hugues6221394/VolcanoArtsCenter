CREATE TABLE IF NOT EXISTS schema_version_marker (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_version_marker (name)
VALUES ('volcano-baseline')
ON CONFLICT (name) DO NOTHING;
