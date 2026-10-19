package com.contractcompiler.analysis.persistence;

import com.contractcompiler.contract.ContractVersion;
import jakarta.persistence.*;

/**
 * JPA entity for the {@code cross_reference} table.
 * Represents a cross-reference found in the document text (e.g. "Section 7.4").
 */
@Entity
@Table(name = "cross_reference")
public class CrossReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ContractVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_clause_id", nullable = false)
    private ClauseEntity sourceClause;

    /** The raw reference text, e.g. "Section 7.4" */
    @Column(name = "raw_text", nullable = false, length = 200)
    private String rawText;

    /** The target clause label, e.g. "7.4" */
    @Column(name = "target_label", nullable = false, length = 30)
    private String targetLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_clause_id")
    private ClauseEntity targetClause;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    protected CrossReferenceEntity() {}

    public CrossReferenceEntity(ContractVersion version, ClauseEntity sourceClause,
                                String rawText, String targetLabel,
                                int startOffset, int endOffset) {
        this.version = version;
        this.sourceClause = sourceClause;
        this.rawText = rawText;
        this.targetLabel = targetLabel;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public ContractVersion getVersion() { return version; }
    public ClauseEntity getSourceClause() { return sourceClause; }
    public String getRawText() { return rawText; }
    public String getTargetLabel() { return targetLabel; }
    public ClauseEntity getTargetClause() { return targetClause; }
    public boolean isResolved() { return resolved; }
    public int getStartOffset() { return startOffset; }
    public int getEndOffset() { return endOffset; }

    // --- Setters ---

    public void resolve(ClauseEntity targetClause) {
        this.targetClause = targetClause;
        this.resolved = true;
    }
}
