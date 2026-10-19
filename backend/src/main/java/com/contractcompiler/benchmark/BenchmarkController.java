package com.contractcompiler.benchmark;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for benchmark results.
 * Read-only — runs are created by the BenchmarkRunner (test/CI tooling), not via API.
 * Admin-only access.
 */
@RestController
@RequestMapping("/api/benchmarks")
@Tag(name = "Benchmarks", description = "Precision/recall benchmark results")
public class BenchmarkController {

    private final BenchmarkRunRepository runRepository;

    public BenchmarkController(BenchmarkRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    @GetMapping
    @Operation(summary = "List recent benchmark runs (latest 10)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BenchmarkRunResponse>> listRecentRuns() {
        List<BenchmarkRunResponse> runs = runRepository.findTop10ByOrderByRanAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific benchmark run with per-rule results")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BenchmarkRunResponse> getRun(@PathVariable Long id) {
        return runRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private BenchmarkRunResponse toResponse(BenchmarkRun run) {
        List<BenchmarkRunResponse.BenchmarkResultResponse> results = run.getResults().stream()
                .map(r -> new BenchmarkRunResponse.BenchmarkResultResponse(
                        r.getRuleId(), r.getExpected(), r.getDetected(),
                        r.getTruePositive(), r.getFalsePositive(), r.getFalseNegative()
                ))
                .collect(Collectors.toList());

        return new BenchmarkRunResponse(
                run.getId(), run.getDatasetName(), run.getGitCommit(),
                run.getTotalExpected(), run.getTotalDetected(),
                run.getTruePositive(), run.getFalsePositive(), run.getFalseNegative(),
                run.getPrecisionPct(), run.getRecallPct(), run.getRanAt(),
                results
        );
    }
}
