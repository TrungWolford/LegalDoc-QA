package com.contractcompiler.finding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Response body for findings in GET /api/versions/{vid}/findings.
 */
public record FindingResponse(
        Long id,
        String ruleId,
        String source,
        String severity,
        String status,
        boolean needsCounsel,
        String sectionLabel,
        String evidence,
        Integer evidenceStart,
        Integer evidenceEnd,
        Integer page,
        String message,
        Map<String, Object> details,
        BigDecimal confidence,
        String fingerprint,
        Long carriedFromId,
        String resolutionNote,
        String assigneeName,
        long lockVersion,
        Instant createdAt,
        Instant updatedAt
) {}
