package com.contractcompiler.benchmark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link BenchmarkRun} entities.
 */
@Repository
public interface BenchmarkRunRepository extends JpaRepository<BenchmarkRun, Long> {

    List<BenchmarkRun> findByDatasetNameOrderByRanAtDesc(String datasetName);

    List<BenchmarkRun> findTop10ByOrderByRanAtDesc();
}
