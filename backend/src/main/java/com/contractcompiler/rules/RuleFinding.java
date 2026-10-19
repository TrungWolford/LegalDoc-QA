package com.contractcompiler.rules;

import com.contractcompiler.finding.Severity;

import java.util.Map;

/**
 * Immutable value object returned by a {@link Rule} before persistence.
 * The engine converts these into persisted {@code Finding} entities with fingerprints.
 *
 * @param ruleId       stable rule identifier from {@link RuleIds}
 * @param severity     effective severity
 * @param clauseId     the clause where the issue was found (may be null for document-level findings)
 * @param sectionLabel human-readable section label (e.g. "2.3" or "2.1 vs 2.2")
 * @param evidence     verbatim evidence text extracted from the document
 * @param evidenceStart start offset in the full text (inclusive)
 * @param evidenceEnd   end offset in the full text (exclusive)
 * @param page         page number (nullable for DOCX)
 * @param message      human-readable, actionable description
 * @param details      rule-specific structured details (JSONB)
 * @param confidence   confidence score 0.0–1.0 (1.0 for deterministic rules)
 */
public record RuleFinding(
        String ruleId,
        Severity severity,
        Long clauseId,
        String sectionLabel,
        String evidence,
        int evidenceStart,
        int evidenceEnd,
        Integer page,
        String message,
        Map<String, Object> details,
        double confidence
) {
    public RuleFinding {
        if (ruleId == null || ruleId.isBlank()) throw new IllegalArgumentException("ruleId is required");
        if (severity == null) throw new IllegalArgumentException("severity is required");
        if (sectionLabel == null || sectionLabel.isBlank()) throw new IllegalArgumentException("sectionLabel is required");
        if (evidence == null || evidence.isBlank()) throw new IllegalArgumentException("evidence is required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message is required");
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence must be 0.0–1.0");
        if (evidenceStart < 0) throw new IllegalArgumentException("evidenceStart must be >= 0");
        if (evidenceEnd < evidenceStart) throw new IllegalArgumentException("evidenceEnd must be >= evidenceStart");
        if (details == null) details = Map.of();
    }
}
