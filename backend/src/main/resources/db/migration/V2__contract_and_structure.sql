-- V2: Contract, version, and parsed structure tables

CREATE TABLE contract (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    status      VARCHAR(30)  NOT NULL DEFAULT 'IN_REVIEW'
                CHECK (status IN ('IN_REVIEW', 'READY_FOR_DIGITIZATION', 'ARCHIVED')),
    created_by  BIGINT       NOT NULL REFERENCES app_user(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE contract_version (
    id              BIGSERIAL PRIMARY KEY,
    contract_id     BIGINT       NOT NULL REFERENCES contract(id) ON DELETE CASCADE,
    version_no      INT          NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(10)  NOT NULL CHECK (file_type IN ('DOCX', 'PDF')),
    storage_path    VARCHAR(500) NOT NULL,
    file_sha256     CHAR(64)     NOT NULL,
    page_count      INT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ANALYZING'
                    CHECK (status IN ('ANALYZING', 'REVIEW_READY', 'FAILED')),
    unparsed_lines  INT          NOT NULL DEFAULT 0,
    failure_reason  TEXT,
    uploaded_by     BIGINT       NOT NULL REFERENCES app_user(id),
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    analyzed_at     TIMESTAMPTZ,
    UNIQUE (contract_id, version_no)
);

-- Clause tree (Article → Section → Clause) stored as adjacency list
CREATE TABLE clause (
    id            BIGSERIAL PRIMARY KEY,
    version_id    BIGINT      NOT NULL REFERENCES contract_version(id) ON DELETE CASCADE,
    parent_id     BIGINT      REFERENCES clause(id) ON DELETE CASCADE,
    level         SMALLINT    NOT NULL,
    label         VARCHAR(30) NOT NULL,
    title         VARCHAR(255),
    body          TEXT        NOT NULL,
    seq           INT         NOT NULL,
    page          INT,
    start_offset  INT         NOT NULL,
    end_offset    INT         NOT NULL,
    UNIQUE (version_id, seq)
);
CREATE INDEX idx_clause_version_label ON clause(version_id, label);
CREATE INDEX idx_clause_parent        ON clause(parent_id);

CREATE TABLE defined_term (
    id                   BIGSERIAL PRIMARY KEY,
    version_id           BIGINT       NOT NULL REFERENCES contract_version(id) ON DELETE CASCADE,
    term                 VARCHAR(200) NOT NULL,
    definition_clause_id BIGINT       REFERENCES clause(id) ON DELETE SET NULL,
    definition_text      TEXT         NOT NULL,
    usage_count          INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_term_version ON defined_term(version_id, term);

CREATE TABLE cross_reference (
    id               BIGSERIAL PRIMARY KEY,
    version_id       BIGINT       NOT NULL REFERENCES contract_version(id) ON DELETE CASCADE,
    source_clause_id BIGINT       NOT NULL REFERENCES clause(id) ON DELETE CASCADE,
    raw_text         VARCHAR(200) NOT NULL,
    target_label     VARCHAR(30)  NOT NULL,
    target_clause_id BIGINT       REFERENCES clause(id) ON DELETE SET NULL,
    resolved         BOOLEAN      NOT NULL DEFAULT FALSE,
    start_offset     INT          NOT NULL,
    end_offset       INT          NOT NULL
);
CREATE INDEX idx_ref_version_resolved ON cross_reference(version_id, resolved);

CREATE TABLE extracted_value (
    id            BIGSERIAL PRIMARY KEY,
    version_id    BIGINT       NOT NULL REFERENCES contract_version(id) ON DELETE CASCADE,
    clause_id     BIGINT       NOT NULL REFERENCES clause(id) ON DELETE CASCADE,
    value_type    VARCHAR(20)  NOT NULL
                  CHECK (value_type IN ('MONEY', 'PERCENT', 'DATE', 'MONEY_WORDS')),
    raw_text      VARCHAR(300) NOT NULL,
    numeric_value NUMERIC(24,6),
    date_value    DATE,
    currency      CHAR(3),
    subject_term  VARCHAR(200),
    start_offset  INT          NOT NULL,
    end_offset    INT          NOT NULL
);
CREATE INDEX idx_value_version_subject ON extracted_value(version_id, subject_term);
CREATE INDEX idx_value_version_type    ON extracted_value(version_id, value_type);
