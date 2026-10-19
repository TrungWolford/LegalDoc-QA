package com.contractcompiler.analysis.model;

/**
 * A defined term extracted from the contract (e.g., "Fund", "Capital Commitment").
 *
 * @param term                 the defined term text
 * @param definitionText       the full text of the definition
 * @param definitionClauseLabel the clause label where defined (e.g., "1.1")
 * @param definitionClauseId   persisted clause ID (nullable before persistence)
 * @param usageCount           number of times the term is used outside its definition
 * @param startOffset          start offset in full text
 * @param endOffset            end offset in full text
 */
public record DefinedTerm(
        String term,
        String definitionText,
        String definitionClauseLabel,
        Long definitionClauseId,
        int usageCount,
        int startOffset,
        int endOffset
) {}
