CREATE TABLE routing_rules (
                               id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               route_id            UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
                               match_config        JSONB NOT NULL,
                               override_target_url VARCHAR(500) NOT NULL,
                               priority            INTEGER NOT NULL DEFAULT 0,
                               enabled             BOOLEAN NOT NULL DEFAULT TRUE,
                               created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_rules_route_id ON routing_rules(route_id);
CREATE INDEX idx_routing_rules_priority ON routing_rules(priority ASC);