package com.contractcompiler.counsel.repository;

import com.contractcompiler.counsel.QuestionPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link QuestionPackage} entities.
 */
@Repository
public interface QuestionPackageRepository extends JpaRepository<QuestionPackage, Long> {

    List<QuestionPackage> findByContractIdOrderByCreatedAtDesc(Long contractId);

    Optional<QuestionPackage> findByTokenHash(String tokenHash);
}
