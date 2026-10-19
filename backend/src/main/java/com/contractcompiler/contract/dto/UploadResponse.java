package com.contractcompiler.contract.dto;

import java.time.Instant;

/**
 * Response body returned after creating a contract or uploading a new version.
 */
public record UploadResponse(
        Long contractId,
        Long versionId,
        int versionNo,
        String status,
        int findingCount,
        Instant uploadedAt
) {}
