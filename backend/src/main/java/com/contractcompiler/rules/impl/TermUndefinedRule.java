package com.contractcompiler.rules.impl;

import com.contractcompiler.analysis.model.ContractModel;
import com.contractcompiler.analysis.model.DefinedTerm;
import com.contractcompiler.finding.Severity;
import com.contractcompiler.rules.Rule;
import com.contractcompiler.rules.RuleFinding;
import com.contractcompiler.rules.RuleIds;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * TERM_UNDEFINED — detects capitalized terms used in the body text
 * (appearing 2+ times) that are never formally defined in the contract.
 *
 * <p>Heuristic: a term with ≥2 capitalized words that is NOT in the
 * defined-term registry is flagged as potentially undefined.</p>
 */
@Component
public class TermUndefinedRule implements Rule {

    private static final int DEFAULT_MIN_OCCURRENCES = 2;

    // Regex: capitalized multi-word term
    private static final Pattern CAP_TERM = Pattern.compile(
            "\\b([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯ][a-zàáâãèéêìíòóôõùúăđĩũơư]+(?:\\s+[A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯ][a-zàáâãèéêìíòóôõùúăđĩũơư]+)+)\\b",
            Pattern.UNICODE_CASE);

    // Common words to exclude (not contract terms)
    private static final java.util.Set<String> STOPWORDS = java.util.Set.of(
            "This Agreement", "The Parties", "The Party", "The Company",
            "Bên Mua", "Bên Bán", "Các Bên", "Hợp Đồng"
    );

    @Override
    public String id() {
        return RuleIds.TERM_UNDEFINED;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.WARNING;
    }

    @Override
    public List<RuleFinding> check(ContractModel model) {
        java.util.Set<String> definedNames = model.definedTermNames();
        String fullText = model.fullText();

        // Count occurrences of capitalized multi-word terms NOT in defined set
        java.util.Map<String, Integer> undefinedCounts = new java.util.LinkedHashMap<>();
        java.util.regex.Matcher m = CAP_TERM.matcher(fullText);
        while (m.find()) {
            String term = m.group(1);
            if (!definedNames.contains(term) && !STOPWORDS.contains(term)) {
                undefinedCounts.merge(term, 1, Integer::sum);
            }
        }

        return undefinedCounts.entrySet().stream()
                .filter(e -> e.getValue() >= DEFAULT_MIN_OCCURRENCES)
                .map(e -> toFinding(e.getKey(), e.getValue(), fullText))
                .toList();
    }

    private RuleFinding toFinding(String term, int occurrences, String fullText) {
        // Find first occurrence for evidence
        int idx = fullText.indexOf(term);
        int start = Math.max(0, idx);
        int end = Math.min(fullText.length(), idx + term.length());
        String evidence = idx >= 0 ? fullText.substring(start, end) : term;

        return new RuleFinding(
                RuleIds.TERM_UNDEFINED,
                Severity.WARNING,
                null,
                "document",
                evidence,
                start,
                end,
                null,
                "Term '" + term + "' appears " + occurrences + " time(s) but is not formally defined in the contract.",
                Map.of("term", term, "occurrences", occurrences),
                0.8
        );
    }
}
