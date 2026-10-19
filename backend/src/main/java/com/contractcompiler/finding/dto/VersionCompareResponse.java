package com.contractcompiler.finding.dto;

import java.util.List;

/**
 * Response body for GET /api/versions/{vid}/compare/{otherVid}.
 * Groups findings by their cross-version status.
 */
public record VersionCompareResponse(
        Long baseVersionId,
        Long compareVersionId,

        /** Findings that exist in both versions (carried over) */
        List<FindingResponse> stillOpen,

        /** Findings only in the base version (resolved in new version) */
        List<FindingResponse> resolved,

        /** Findings only in the compare version (new issues) */
        List<FindingResponse> newFindings
) {}
