-- V3: Finding and audit trail tables

CREATE TABLE finding (
    id                 BIGSERIAL PRIMARY KEY,
    contract_id        BIGINT       NOT NULL REFERENCES contract(id) ON DELETE CASCADE,
    version_id         BIGINT       NOT NULL REFERENCES contract_version(id) ON DELETE CASCADE,
    rule_id            VARCHAR(60)  NOT NULL,
    source             VARCHAR(10)  NOT NULL DEFAULT 'RULE' CHECK (source IN ('RULE', 'AI')),
    severity           VARCHAR(10)  NOT NULL CHECK (severity IN ('ERROR', 'WARNING', 'INFO')),
    status             VARCHAR(20)  NOT NULL DEFAULT 'DETECTED'
                       CHECK (status IN ('DETECTED', 'CONFIRMED', 'SENT_TO_COUNSEL',
                                         'RESOLVED', 'DISMISSED')),
    needs_counsel      BOOLEAN      NOT NULL DEFAULT FALSE,
    clause_id          BIGINT       REFERENCES clause(id) ON DELETE SET NULL,
    section_label      VARCHAR(60)  NOT NULL,
    evidence           TEXT         NOT NULL,
    evidence_start     INT,
    evidence_end       INT,
    page               INT,
    message            TEXT         NOT NULL,
    details            JSONB,
    confidence         NUMERIC(3,2) NOT NULL DEFAULT 1.00,
    fingerprint        CHAR(64)     NOT NULL,
    carried_from_id    BIGINT       REFERENCES finding(id) ON DELETE SET NULL,
    resolution_note    TEXT,
    assignee_id        BIGINT       REFERENCES app_user(id),
    lock_version       BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_finding_version_status ON finding(version_id, status);
CREATE INDEX idx_finding_version_sev    ON finding(version_id, severity);
CREATE INDEX idx_finding_rule           ON finding(rule_id);
CREATE INDEX idx_finding_fingerprint    ON finding(contract_id, fingerprint);

-- Audit trail: INSERT only, no UPDATE/DELETE
CREATE TABLE finding_event (
    id           BIGSERIAL PRIMARY KEY,
    finding_id   BIGINT      NOT NULL REFERENCES finding(id) ON DELETE CASCADE,
    action       VARCHAR(30) NOT NULL
                 CHECK (action IN ('CREATED', 'STATUS_CHANGED', 'COMMENTED', 'ASSIGNED',
                                   'SENT_TO_COUNSEL', 'COUNSEL_ANSWERED', 'CARRIED_OVER')),
    from_status  VARCHAR(20),
    to_status    VARCHAR(20),
    actor_type   VARCHAR(10) NOT NULL CHECK (actor_type IN ('USER', 'COUNSEL', 'SYSTEM')),
    actor_id     BIGINT      REFERENCES app_user(id),
    note         TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_event_finding ON finding_event(finding_id, created_at);
