package com.contractcompiler.analysis.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link ClauseEntity}.
 */
@Repository
public interface ClauseRepository extends JpaRepository<ClauseEntity, Long> {

    /**
     * Loads the full clause tree for a version, ordered by sequence.
     * Includes children eagerly for tree construction.
     */
    @Query("SELECT c FROM ClauseEntity c LEFT JOIN FETCH c.children WHERE c.version.id = :versionId AND c.parent IS NULL ORDER BY c.seq")
    List<ClauseEntity> findRootsByVersionId(Long versionId);

    List<ClauseEntity> findByVersionIdOrderBySeqAsc(Long versionId);

    void deleteByVersionId(Long versionId);
}
