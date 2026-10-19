-- V1: Users table
-- Supports REVIEWER and ADMIN roles with BCrypt password hashing.

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('REVIEWER', 'ADMIN')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
