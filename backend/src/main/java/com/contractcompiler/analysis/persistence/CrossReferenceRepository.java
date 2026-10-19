package com.contractcompiler.analysis.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link CrossReferenceEntity}.
 */
@Repository
public interface CrossReferenceRepository extends JpaRepository<CrossReferenceEntity, Long> {

    List<CrossReferenceEntity> findByVersionId(Long versionId);

    List<CrossReferenceEntity> findByVersionIdAndResolvedFalse(Long versionId);

    void deleteByVersionId(Long versionId);
}
