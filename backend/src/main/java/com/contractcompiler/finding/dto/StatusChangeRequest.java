package com.contractcompiler.finding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PATCH /api/findings/{id}/status.
 * Changes the status of a finding in the workflow state machine.
 */
public record StatusChangeRequest(
        @NotBlank(message = "Target status is required")
        String toStatus,

        /** Optional note explaining the status change */
        String note,

        /**
         * Optimistic lock version from the finding.
         * Must match current version or the request is rejected.
         */
        @NotNull(message = "Lock version is required for concurrent modification check")
        Long version
) {}
