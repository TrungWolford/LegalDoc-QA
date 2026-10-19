package com.contractcompiler.counsel.repository;

import com.contractcompiler.counsel.QuestionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link QuestionItem} entities.
 */
@Repository
public interface QuestionItemRepository extends JpaRepository<QuestionItem, Long> {

    List<QuestionItem> findByQuestionPackageIdOrderByPositionAsc(Long packageId);

    Optional<QuestionItem> findByQuestionPackageIdAndPosition(Long packageId, int position);
}
