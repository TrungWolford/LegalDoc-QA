package com.contractcompiler.benchmark;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code benchmark_run} table.
 * Records the results of running the rule engine against a known test dataset.
 */
@Entity
@Table(name = "benchmark_run")
public class BenchmarkRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_name", nullable = false, length = 150)
    private String datasetName;

    @Column(name = "git_commit", length = 40)
    private String gitCommit;

    @Column(name = "total_expected", nullable = false)
    private int totalExpected;

    @Column(name = "total_detected", nullable = false)
    private int totalDetected;

    @Column(name = "true_positive", nullable = false)
    private int truePositive;

    @Column(name = "false_positive", nullable = false)
    private int falsePositive;

    @Column(name = "false_negative", nullable = false)
    private int falseNegative;

    @Column(name = "precision_pct", precision = 5, scale = 2)
    private BigDecimal precisionPct;

    @Column(name = "recall_pct", precision = 5, scale = 2)
    private BigDecimal recallPct;

    @Column(name = "ran_at", nullable = false, updatable = false)
    private Instant ranAt = Instant.now();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BenchmarkResult> results = new ArrayList<>();

    protected BenchmarkRun() {}

    public BenchmarkRun(String datasetName, String gitCommit, int totalExpected,
                        int totalDetected, int truePositive, int falsePositive,
                        int falseNegative) {
        this.datasetName = datasetName;
        this.gitCommit = gitCommit;
        this.totalExpected = totalExpected;
        this.totalDetected = totalDetected;
        this.truePositive = truePositive;
        this.falsePositive = falsePositive;
        this.falseNegative = falseNegative;
        this.precisionPct = totalDetected > 0
                ? BigDecimal.valueOf(truePositive * 100.0 / totalDetected)
                : null;
        this.recallPct = totalExpected > 0
                ? BigDecimal.valueOf(truePositive * 100.0 / totalExpected)
                : null;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public String getDatasetName() { return datasetName; }
    public String getGitCommit() { return gitCommit; }
    public int getTotalExpected() { return totalExpected; }
    public int getTotalDetected() { return totalDetected; }
    public int getTruePositive() { return truePositive; }
    public int getFalsePositive() { return falsePositive; }
    public int getFalseNegative() { return falseNegative; }
    public BigDecimal getPrecisionPct() { return precisionPct; }
    public BigDecimal getRecallPct() { return recallPct; }
    public Instant getRanAt() { return ranAt; }
    public List<BenchmarkResult> getResults() { return results; }

    public void addResult(BenchmarkResult result) {
        results.add(result);
        result.setRun(this);
    }
}
