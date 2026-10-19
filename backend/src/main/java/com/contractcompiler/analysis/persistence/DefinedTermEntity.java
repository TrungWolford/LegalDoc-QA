package com.contractcompiler.analysis.persistence;

import com.contractcompiler.contract.ContractVersion;
import jakarta.persistence.*;

/**
 * JPA entity for the {@code defined_term} table.
 * Represents a defined term found in the contract (e.g. from the Definitions article).
 */
@Entity
@Table(name = "defined_term")
public class DefinedTermEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ContractVersion version;

    @Column(nullable = false, length = 200)
    private String term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_clause_id")
    private ClauseEntity definitionClause;

    @Column(name = "definition_text", nullable = false, columnDefinition = "TEXT")
    private String definitionText;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    protected DefinedTermEntity() {}

    public DefinedTermEntity(ContractVersion version, String term,
                             ClauseEntity definitionClause, String definitionText) {
        this.version = version;
        this.term = term;
        this.definitionClause = definitionClause;
        this.definitionText = definitionText;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public ContractVersion getVersion() { return version; }
    public String getTerm() { return term; }
    public ClauseEntity getDefinitionClause() { return definitionClause; }
    public String getDefinitionText() { return definitionText; }
    public int getUsageCount() { return usageCount; }

    // --- Setters ---

    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
}
