package com.contractcompiler.analysis.parse;

import java.util.Collections;
import java.util.List;

/**
 * Report produced by {@link ClauseTreeBuilder} documenting parse quality.
 *
 * @param totalLines    total input lines
 * @param parsedLines   lines successfully assigned to clauses
 * @param unparsedLines lines the parser could not recognize
 * @param unparsedTexts the actual text of unparsed lines (for diagnostics)
 * @param coveragePercent percentage of lines successfully parsed
 */
public record ParseReport(
        int totalLines,
        int parsedLines,
        int unparsedLines,
        List<String> unparsedTexts,
        double coveragePercent
) {
    public ParseReport {
        if (unparsedTexts == null) unparsedTexts = Collections.emptyList();
    }

    public boolean isBelowThreshold(int thresholdPercent) {
        return coveragePercent < thresholdPercent;
    }
}
