package com.contractcompiler.analysis.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A node in the clause tree representing an Article, Section, or Sub-clause.
 * Immutable once built by {@link ClauseTreeBuilder}.
 */
public final class ClauseNode {

    private final int level;
    private final String label;
    private final String title;
    private final String body;
    private final int seq;
    private final Integer page;
    private final int startOffset;
    private final int endOffset;
    private final List<ClauseNode> children;
    private Long persistedId;

    public ClauseNode(int level, String label, String title, String body,
                      int seq, Integer page, int startOffset, int endOffset) {
        this.level = level;
        this.label = label;
        this.title = title;
        this.body = body;
        this.seq = seq;
        this.page = page;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.children = new ArrayList<>();
    }

    public void addChild(ClauseNode child) {
        this.children.add(child);
    }

    public int level() { return level; }
    public String label() { return label; }
    public String title() { return title; }
    public String body() { return body; }
    public int seq() { return seq; }
    public Integer page() { return page; }
    public int startOffset() { return startOffset; }
    public int endOffset() { return endOffset; }
    public List<ClauseNode> children() { return Collections.unmodifiableList(children); }

    /** Set after persisting the clause entity. Used to link findings to clauses. */
    public void setPersistedId(Long id) { this.persistedId = id; }
    public Long persistedId() { return persistedId; }

    /**
     * Returns a flat list of this node and all descendants in document order.
     */
    public List<ClauseNode> flatten() {
        List<ClauseNode> result = new ArrayList<>();
        result.add(this);
        for (ClauseNode child : children) {
            result.addAll(child.flatten());
        }
        return result;
    }

    @Override
    public String toString() {
        return "ClauseNode{level=%d, label='%s', title='%s', offsets=[%d,%d]}"
                .formatted(level, label, title, startOffset, endOffset);
    }
}
