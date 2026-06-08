ALTER TABLE routes
    ADD COLUMN rate_limit_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN rate_limit_replenish_rate INTEGER,
    ADD COLUMN rate_limit_burst_capacity INTEGER,
    ADD COLUMN rate_limit_requested_tokens INTEGER;