package com.contractcompiler.benchmark;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for benchmark run results.
 */
public record BenchmarkRunResponse(
        Long id,
        String datasetName,
        String gitCommit,
        int totalExpected,
        int totalDetected,
        int truePositive,
        int falsePositive,
        int falseNegative,
        BigDecimal precisionPct,
        BigDecimal recallPct,
        Instant ranAt,
        List<BenchmarkResultResponse> results
) {
    /**
     * Per-rule metrics within a benchmark run.
     */
    public record BenchmarkResultResponse(
            String ruleId,
            int expected,
            int detected,
            int truePositive,
            int falsePositive,
            int falseNegative
    ) {}
}
