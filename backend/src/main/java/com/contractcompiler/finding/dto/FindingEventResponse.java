package com.contractcompiler.finding.dto;

import java.time.Instant;

/**
 * Response body for GET /api/findings/{id}/events.
 * Represents a single audit trail entry for a finding.
 */
public record FindingEventResponse(
        Long id,
        String action,
        String fromStatus,
        String toStatus,
        String actorType,
        String actorName,
        String note,
        Instant createdAt
) {}
