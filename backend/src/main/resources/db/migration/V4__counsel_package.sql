-- V4: Counsel question package and items

CREATE TABLE question_package (
    id               BIGSERIAL PRIMARY KEY,
    contract_id      BIGINT       NOT NULL REFERENCES contract(id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    counsel_name     VARCHAR(150),
    counsel_email    VARCHAR(255) NOT NULL,
    status           VARCHAR(15)  NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'SENT', 'PARTIAL', 'COMPLETED', 'EXPIRED')),
    token_hash       CHAR(64),
    token_expires_at TIMESTAMPTZ,
    send_count       INT          NOT NULL DEFAULT 0,
    created_by       BIGINT       NOT NULL REFERENCES app_user(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at          TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_package_token ON question_package(token_hash) WHERE token_hash IS NOT NULL;

CREATE TABLE question_item (
    id              BIGSERIAL PRIMARY KEY,
    package_id      BIGINT      NOT NULL REFERENCES question_package(id) ON DELETE CASCADE,
    finding_id      BIGINT      NOT NULL REFERENCES finding(id) ON DELETE CASCADE,
    position        INT         NOT NULL,
    question_text   TEXT        NOT NULL,
    options         JSONB       NOT NULL,
    answer_choice   VARCHAR(255),
    answer_note     TEXT,
    answered_at     TIMESTAMPTZ,
    UNIQUE (package_id, position),
    UNIQUE (package_id, finding_id)
);
CREATE INDEX idx_item_finding ON question_item(finding_id);
