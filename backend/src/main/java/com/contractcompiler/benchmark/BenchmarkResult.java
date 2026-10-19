package com.contractcompiler.benchmark;

import jakarta.persistence.*;

/**
 * JPA entity for the {@code benchmark_result} table.
 * Stores per-rule metrics within a benchmark run.
 */
@Entity
@Table(name = "benchmark_result",
       uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "rule_id"}))
public class BenchmarkResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private BenchmarkRun run;

    @Column(name = "rule_id", nullable = false, length = 60)
    private String ruleId;

    @Column(nullable = false)
    private int expected;

    @Column(nullable = false)
    private int detected;

    @Column(name = "true_positive", nullable = false)
    private int truePositive;

    @Column(name = "false_positive", nullable = false)
    private int falsePositive;

    @Column(name = "false_negative", nullable = false)
    private int falseNegative;

    protected BenchmarkResult() {}

    public BenchmarkResult(String ruleId, int expected, int detected,
                           int truePositive, int falsePositive, int falseNegative) {
        this.ruleId = ruleId;
        this.expected = expected;
        this.detected = detected;
        this.truePositive = truePositive;
        this.falsePositive = falsePositive;
        this.falseNegative = falseNegative;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public BenchmarkRun getRun() { return run; }
    public String getRuleId() { return ruleId; }
    public int getExpected() { return expected; }
    public int getDetected() { return detected; }
    public int getTruePositive() { return truePositive; }
    public int getFalsePositive() { return falsePositive; }
    public int getFalseNegative() { return falseNegative; }

    void setRun(BenchmarkRun run) { this.run = run; }
}
