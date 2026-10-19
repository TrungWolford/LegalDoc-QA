-- V6: Benchmark tables

CREATE TABLE benchmark_run (
    id             BIGSERIAL PRIMARY KEY,
    dataset_name   VARCHAR(150) NOT NULL,
    git_commit     VARCHAR(40),
    total_expected INT          NOT NULL,
    total_detected INT          NOT NULL,
    true_positive  INT          NOT NULL,
    false_positive INT          NOT NULL,
    false_negative INT          NOT NULL,
    precision_pct  NUMERIC(5,2),
    recall_pct     NUMERIC(5,2),
    ran_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE benchmark_result (
    id             BIGSERIAL PRIMARY KEY,
    run_id         BIGINT      NOT NULL REFERENCES benchmark_run(id) ON DELETE CASCADE,
    rule_id        VARCHAR(60) NOT NULL,
    expected       INT         NOT NULL,
    detected       INT         NOT NULL,
    true_positive  INT         NOT NULL,
    false_positive INT         NOT NULL,
    false_negative INT         NOT NULL,
    UNIQUE (run_id, rule_id)
);
