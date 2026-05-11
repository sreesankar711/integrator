CREATE TABLE routes (
                        id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name                 VARCHAR(100) UNIQUE NOT NULL,
                        description          VARCHAR(2000),
                        path_pattern         VARCHAR(500) NOT NULL,
                        http_method          VARCHAR(10) NOT NULL,
                        target_url           VARCHAR(500) NOT NULL,
                        transform_type       VARCHAR(30) NOT NULL DEFAULT 'NONE',
                        field_mapping_config JSONB,
                        snippet_id           UUID,
                        enabled              BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routes_enabled ON routes(enabled);
CREATE INDEX idx_routes_path_pattern ON routes(path_pattern);
CREATE INDEX idx_routes_http_method ON routes(http_method);