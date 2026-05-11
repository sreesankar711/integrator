CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username    VARCHAR(50) UNIQUE NOT NULL,
                       email       VARCHAR(255) UNIQUE NOT NULL,
                       password    VARCHAR(255) NOT NULL,
                       role        VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
                       enabled     BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);