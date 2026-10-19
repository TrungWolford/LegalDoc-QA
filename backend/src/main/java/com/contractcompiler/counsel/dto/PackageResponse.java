package com.contractcompiler.counsel.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response body for package-related endpoints.
 * Includes the package metadata and its question items.
 */
public record PackageResponse(
        Long id,
        Long contractId,
        String title,
        String counselName,
        String counselEmail,
        String status,
        int sendCount,
        Instant createdAt,
        Instant sentAt,
        Instant completedAt,
        List<QuestionItemResponse> items
) {
    /**
     * A single question item within the package.
     */
    public record QuestionItemResponse(
            Long id,
            Long findingId,
            int position,
            String questionText,
            List<String> options,
            String answerChoice,
            String answerNote,
            Instant answeredAt
    ) {}
}
