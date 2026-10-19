package com.contractcompiler.finding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link FindingEvent} entities.
 * Append-only: no update or delete operations.
 */
@Repository
public interface FindingEventRepository extends JpaRepository<FindingEvent, Long> {

    List<FindingEvent> findByFindingIdOrderByCreatedAtAsc(Long findingId);
}
