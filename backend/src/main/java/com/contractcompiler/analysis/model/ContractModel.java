package com.contractcompiler.analysis.model;

import com.contractcompiler.analysis.parse.ClauseTree;
import com.contractcompiler.analysis.parse.ParseReport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The fully indexed contract model — the sole input to all {@link com.contractcompiler.rules.Rule} implementations.
 *
 * <p>Provides indexed access structures so rules run in O(n) or O(n log n)
 * without scanning the full text:</p>
 * <ul>
 *   <li>{@link #termsByName()} — defined terms indexed by normalized name</li>
 *   <li>{@link #referencesBySourceLabel()} — cross-references grouped by source clause</li>
 *   <li>{@link #valuesBySubject()} — extracted values grouped by subject term</li>
 *   <li>{@link #clauseTree()} — the parsed clause tree with label lookup</li>
 * </ul>
 */
public final class ContractModel {

    private final ClauseTree clauseTree;
    private final ParseReport parseReport;
    private final List<DefinedTerm> definedTerms;
    private final List<CrossReference> crossReferences;
    private final List<ExtractedValue> extractedValues;
    private final String fullText;

    // Indexed maps (built lazily)
    private Map<String, List<DefinedTerm>> termsByNameCache;
    private Map<String, List<CrossReference>> refsBySourceCache;
    private Map<String, List<ExtractedValue>> valuesBySubjectCache;
    private Map<String, List<ExtractedValue>> valuesByTypeCache;

    public ContractModel(ClauseTree clauseTree,
                         ParseReport parseReport,
                         List<DefinedTerm> definedTerms,
                         List<CrossReference> crossReferences,
                         List<ExtractedValue> extractedValues) {
        this.clauseTree = Objects.requireNonNull(clauseTree);
        this.parseReport = Objects.requireNonNull(parseReport);
        this.definedTerms = Collections.unmodifiableList(definedTerms != null ? definedTerms : List.of());
        this.crossReferences = Collections.unmodifiableList(crossReferences != null ? crossReferences : List.of());
        this.extractedValues = Collections.unmodifiableList(extractedValues != null ? extractedValues : List.of());
        this.fullText = clauseTree.fullText();
    }

    public ClauseTree clauseTree() { return clauseTree; }
    public ParseReport parseReport() { return parseReport; }
    public List<DefinedTerm> definedTerms() { return definedTerms; }
    public List<CrossReference> crossReferences() { return crossReferences; }
    public List<ExtractedValue> extractedValues() { return extractedValues; }
    public String fullText() { return fullText; }

    /**
     * Defined terms indexed by normalized (lowercased) term name.
     * Multiple entries mean duplicate definitions.
     */
    public Map<String, List<DefinedTerm>> termsByName() {
        if (termsByNameCache == null) {
            termsByNameCache = definedTerms.stream()
                    .collect(Collectors.groupingBy(
                            dt -> dt.term().toLowerCase(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
        }
        return termsByNameCache;
    }

    /**
     * Cross-references grouped by source clause label.
     */
    public Map<String, List<CrossReference>> referencesBySourceLabel() {
        if (refsBySourceCache == null) {
            refsBySourceCache = crossReferences.stream()
                    .collect(Collectors.groupingBy(
                            CrossReference::sourceClauseLabel,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
        }
        return refsBySourceCache;
    }

    /**
     * Extracted values grouped by normalized subject term.
     */
    public Map<String, List<ExtractedValue>> valuesBySubject() {
        if (valuesBySubjectCache == null) {
            valuesBySubjectCache = extractedValues.stream()
                    .filter(v -> v.subjectTerm() != null && !v.subjectTerm().isBlank())
                    .collect(Collectors.groupingBy(
                            v -> v.subjectTerm().toLowerCase(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
        }
        return valuesBySubjectCache;
    }

    /**
     * Extracted values grouped by value type name.
     */
    public Map<String, List<ExtractedValue>> valuesByType() {
        if (valuesByTypeCache == null) {
            valuesByTypeCache = extractedValues.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.valueType().name(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
        }
        return valuesByTypeCache;
    }

    /**
     * All labels that exist in the clause tree (for reference resolution).
     */
    public Set<String> allClauseLabels() {
        return clauseTree.allLabels();
    }

    /**
     * Set of all defined term names (original case).
     */
    public Set<String> definedTermNames() {
        return definedTerms.stream()
                .map(DefinedTerm::term)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
