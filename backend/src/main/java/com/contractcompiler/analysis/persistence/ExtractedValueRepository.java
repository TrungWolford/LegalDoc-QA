package com.contractcompiler.analysis.persistence;

import com.contractcompiler.analysis.model.ValueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link ExtractedValueEntity}.
 */
@Repository
public interface ExtractedValueRepository extends JpaRepository<ExtractedValueEntity, Long> {

    List<ExtractedValueEntity> findByVersionId(Long versionId);

    List<ExtractedValueEntity> findByVersionIdAndValueType(Long versionId, ValueType valueType);

    List<ExtractedValueEntity> findByVersionIdAndSubjectTermIsNotNull(Long versionId);

    void deleteByVersionId(Long versionId);
}
