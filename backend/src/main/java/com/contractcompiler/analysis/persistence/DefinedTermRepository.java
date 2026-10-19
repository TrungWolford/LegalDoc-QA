package com.contractcompiler.analysis.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link DefinedTermEntity}.
 */
@Repository
public interface DefinedTermRepository extends JpaRepository<DefinedTermEntity, Long> {

    List<DefinedTermEntity> findByVersionId(Long versionId);

    void deleteByVersionId(Long versionId);
}
