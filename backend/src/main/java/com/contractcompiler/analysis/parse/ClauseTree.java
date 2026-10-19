package com.contractcompiler.analysis.parse;

import java.util.*;

/**
 * The parsed clause tree of a contract document.
 * Provides indexed access by label for O(1) lookups used by rules.
 */
public final class ClauseTree {

    private final List<ClauseNode> roots;
    private final Map<String, ClauseNode> byLabel;
    private final String fullText;

    public ClauseTree(List<ClauseNode> roots, String fullText) {
        this.roots = Collections.unmodifiableList(roots);
        this.fullText = fullText;
        this.byLabel = new LinkedHashMap<>();
        for (ClauseNode root : roots) {
            for (ClauseNode node : root.flatten()) {
                byLabel.put(node.label(), node);
            }
        }
    }

    /** Top-level nodes (Articles). */
    public List<ClauseNode> roots() { return roots; }

    /** The concatenated full text of the document. */
    public String fullText() { return fullText; }

    /** All clause labels in document order. */
    public Set<String> allLabels() { return Collections.unmodifiableSet(byLabel.keySet()); }

    /** Lookup a clause by its label (e.g., "2.3", "1", "2.1(a)"). */
    public Optional<ClauseNode> findByLabel(String label) {
        return Optional.ofNullable(byLabel.get(label));
    }

    /** All nodes flattened in document order. */
    public List<ClauseNode> allNodes() {
        List<ClauseNode> all = new ArrayList<>();
        for (ClauseNode root : roots) {
            all.addAll(root.flatten());
        }
        return all;
    }

    /** Total number of clauses. */
    public int size() { return byLabel.size(); }
}
