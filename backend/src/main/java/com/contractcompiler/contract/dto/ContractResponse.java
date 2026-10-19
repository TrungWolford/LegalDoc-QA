package com.contractcompiler.contract.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response body for GET /api/contracts and GET /api/contracts/{id}.
 * Summary view of a contract with its versions.
 */
public record ContractResponse(
        Long id,
        String title,
        String status,
        String createdByName,
        Instant createdAt,
        Instant updatedAt,
        List<VersionSummary> versions
) {
    /**
     * Compact version summary embedded in the contract response.
     */
    public record VersionSummary(
            Long id,
            int versionNo,
            String fileName,
            String fileType,
            String status,
            int findingCount,
            Instant uploadedAt,
            Instant analyzedAt
    ) {}
}
