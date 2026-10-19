package com.contractcompiler.contract.dto;

import java.util.List;

/**
 * Response body for GET /api/versions/{vid}/text.
 * Contains the clause tree and version metadata for document display.
 */
public record VersionTextResponse(
        Long versionId,
        int versionNo,
        String fileName,
        int pageCount,
        int unparsedLines,
        List<ClauseNode> clauses
) {
    /**
     * A clause node in the document tree for rendering.
     */
    public record ClauseNode(
            Long id,
            short level,
            String label,
            String title,
            String body,
            int page,
            int startOffset,
            int endOffset,
            List<ClauseNode> children
    ) {}
}
