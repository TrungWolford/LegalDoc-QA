package com.contractcompiler.analysis.parse;

import com.contractcompiler.analysis.model.CrossReference;
import com.contractcompiler.analysis.model.DefinedTerm;
import com.contractcompiler.analysis.model.ExtractedValue;
import com.contractcompiler.analysis.model.ValueType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts defined terms, cross-references, and values from a parsed ClauseTree.
 * Operates on the ClauseTree's full text and clause nodes after the tree is built.
 */
@Component
public class ModelBuilder {

    // Term definitions: "Term" means ... / "Term" shall mean ...
    // Matches capitalized terms in quotes: "Capital Commitment" or "Bên Mua"
    private static final Pattern TERM_DEF_PATTERN = Pattern.compile(
            "\"([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯẠ-ỹ][^\"]{1,60})\"\\s+(?:means|shall mean|nghĩa là|được hiểu là)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Capitalized terms used in text (no quotes) — at least 2 capital words
    private static final Pattern TERM_USAGE_PATTERN = Pattern.compile(
            "\\b([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯẠ-ỹ][a-zàáâãèéêìíòóôõùúăđĩũơư]+(?:\\s+[A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯẠ-ỹ][a-zàáâãèéêìíòóôõùúăđĩũơư]+)+)\\b",
            Pattern.UNICODE_CASE);

    // Cross-references: "Điều 5", "Khoản 3", "Section 7.4", "Article 2", "clause 5.1"
    private static final Pattern XREF_PATTERN = Pattern.compile(
            "(?:Điều|Khoản|Mục|Article|Section|Clause|clause)\\s+(\\d+(?:\\.\\d+)*(?:\\([a-z]\\))?)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Money: $1,000,000 or 1.000.000 VND or 1,000,000 USD
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?:\\$|USD|VND|EUR)?\\s*([\\d,\\.]+)\\s*(?:USD|VND|EUR|đồng)?",
            Pattern.CASE_INSENSITIVE);

    // Percentage: 10%, 10.5%
    private static final Pattern PERCENT_PATTERN = Pattern.compile(
            "([\\d]+(?:[\\.,]\\d+)?)\\s*%");

    /**
     * Extracts all semantic entities from the clause tree.
     */
    public ModelBuildResult extract(ClauseTree tree) {
        List<DefinedTerm> definedTerms = new ArrayList<>();
        List<CrossReference> crossReferences = new ArrayList<>();
        List<ExtractedValue> extractedValues = new ArrayList<>();

        String fullText = tree.fullText();

        // Track term usage counts across full text
        Map<String, Integer> termUsageCounts = new LinkedHashMap<>();

        for (ClauseNode node : tree.allNodes()) {
            String body = (node.title() != null ? node.title() + " " : "") +
                          (node.body() != null ? node.body() : "");
            if (body.isBlank()) continue;

            // --- Extract defined terms ---
            Matcher termMatcher = TERM_DEF_PATTERN.matcher(body);
            while (termMatcher.find()) {
                String term = termMatcher.group(1).strip();
                int start = computeOffset(fullText, node.startOffset(), termMatcher.start());
                int end = computeOffset(fullText, node.startOffset(), termMatcher.end());
                definedTerms.add(new DefinedTerm(term, body, node.label(),
                        node.persistedId(), 0, start, end));
                termUsageCounts.putIfAbsent(term.toLowerCase(), 0);
            }

            // --- Extract cross-references ---
            Matcher xrefMatcher = XREF_PATTERN.matcher(body);
            while (xrefMatcher.find()) {
                String targetLabel = xrefMatcher.group(1);
                boolean resolved = tree.findByLabel(targetLabel).isPresent();
                int start = computeOffset(fullText, node.startOffset(), xrefMatcher.start());
                int end = computeOffset(fullText, node.startOffset(), xrefMatcher.end());
                // Check for self-reference
                boolean selfRef = targetLabel.equals(node.label());
                crossReferences.add(new CrossReference(
                        xrefMatcher.group(0), targetLabel,
                        node.label(), node.persistedId(),
                        resolved && !selfRef, start, end));
            }

            // --- Extract percentage values ---
            Matcher pctMatcher = PERCENT_PATTERN.matcher(body);
            while (pctMatcher.find()) {
                try {
                    BigDecimal val = new BigDecimal(pctMatcher.group(1).replace(",", "."));
                    int start = computeOffset(fullText, node.startOffset(), pctMatcher.start());
                    int end = computeOffset(fullText, node.startOffset(), pctMatcher.end());
                    extractedValues.add(new ExtractedValue(
                            ValueType.PERCENT, pctMatcher.group(0),
                            val, null, null, null,
                            node.label(), node.persistedId(), start, end));
                } catch (NumberFormatException ignored) {}
            }
        }

        // --- Count term usages across full text ---
        List<DefinedTerm> termsWithUsage = new ArrayList<>();
        for (DefinedTerm dt : definedTerms) {
            String key = dt.term().toLowerCase();
            int count = countOccurrences(fullText, dt.term());
            // Subtract 1 for the definition itself
            int usageCount = Math.max(0, count - 1);
            termsWithUsage.add(new DefinedTerm(
                    dt.term(), dt.definitionText(), dt.definitionClauseLabel(),
                    dt.definitionClauseId(), usageCount, dt.startOffset(), dt.endOffset()));
        }

        return new ModelBuildResult(termsWithUsage, crossReferences, extractedValues);
    }

    private int computeOffset(String fullText, int clauseStart, int matchOffset) {
        // Approximate: use clause start + match offset within body
        return Math.min(clauseStart + matchOffset, fullText.length());
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        String lowerText = text.toLowerCase();
        String lowerTerm = term.toLowerCase();
        while ((idx = lowerText.indexOf(lowerTerm, idx)) != -1) {
            count++;
            idx += lowerTerm.length();
        }
        return count;
    }

    public record ModelBuildResult(
            List<DefinedTerm> definedTerms,
            List<CrossReference> crossReferences,
            List<ExtractedValue> extractedValues
    ) {}
}
