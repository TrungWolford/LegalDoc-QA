package com.contractcompiler.analysis.model;

/**
 * A cross-reference extracted from the contract (e.g., "Section 7.4", "Article IV").
 *
 * @param rawText          the verbatim reference text (e.g., "Section 7.4")
 * @param targetLabel      the normalized label being referenced (e.g., "7.4")
 * @param sourceClauseLabel the clause containing this reference
 * @param sourceClauseId   persisted clause ID (nullable before persistence)
 * @param resolved         whether the target label exists in the clause tree
 * @param startOffset      start offset in full text
 * @param endOffset        end offset in full text
 */
public record CrossReference(
        String rawText,
        String targetLabel,
        String sourceClauseLabel,
        Long sourceClauseId,
        boolean resolved,
        int startOffset,
        int endOffset
) {}
