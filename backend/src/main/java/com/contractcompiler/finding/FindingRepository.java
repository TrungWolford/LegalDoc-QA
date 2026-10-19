package com.contractcompiler.finding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Finding} entities.
 */
@Repository
public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByVersionIdOrderByCreatedAtAsc(Long versionId);

    @Query("SELECT f FROM Finding f WHERE f.version.id = :versionId " +
           "AND (:severity IS NULL OR f.severity = :severity) " +
           "AND (:status IS NULL OR f.status = :status) " +
           "AND (:ruleId IS NULL OR f.ruleId = :ruleId) " +
           "ORDER BY f.createdAt ASC")
    List<Finding> findByVersionFiltered(Long versionId, Severity severity,
                                        FindingStatus status, String ruleId);

    int countByVersionId(Long versionId);

    /**
     * Finds all findings for a contract+version by fingerprint, used for version comparison.
     */
    @Query("SELECT f FROM Finding f WHERE f.contract.id = :contractId AND f.version.id = :versionId")
    List<Finding> findByContractAndVersion(Long contractId, Long versionId);

    /**
     * Finds open findings (not RESOLVED/DISMISSED) for a contract to determine readiness.
     */
    @Query("SELECT COUNT(f) FROM Finding f WHERE f.contract.id = :contractId " +
           "AND f.status NOT IN (com.contractcompiler.finding.FindingStatus.RESOLVED, " +
           "com.contractcompiler.finding.FindingStatus.DISMISSED)")
    int countOpenByContractId(Long contractId);
}
