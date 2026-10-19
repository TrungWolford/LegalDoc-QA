package com.contractcompiler.finding;

import com.contractcompiler.analysis.persistence.ClauseEntity;
import com.contractcompiler.auth.AppUser;
import com.contractcompiler.contract.Contract;
import com.contractcompiler.contract.ContractVersion;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * JPA entity for the {@code finding} table.
 * Represents an issue detected in a contract version by the rule engine or AI plugin.
 * Uses {@code @Version} for optimistic locking to prevent concurrent modification.
 */
@Entity
@Table(name = "finding")
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ContractVersion version;

    @Column(name = "rule_id", nullable = false, length = 60)
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FindingSource source = FindingSource.RULE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingStatus status = FindingStatus.DETECTED;

    @Column(name = "needs_counsel", nullable = false)
    private boolean needsCounsel = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clause_id")
    private ClauseEntity clause;

    @Column(name = "section_label", nullable = false, length = 60)
    private String sectionLabel;

    /** Verbatim evidence text extracted from the document */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "evidence_start")
    private Integer evidenceStart;

    @Column(name = "evidence_end")
    private Integer evidenceEnd;

    @Column
    private Integer page;

    /** Human-readable, actionable description of the issue */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Rule-specific structured data (stored as JSONB) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    /** Confidence score 0.00–1.00 (1.00 for deterministic rules) */
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence = BigDecimal.ONE;

    /** SHA-256 fingerprint for cross-version finding identity */
    @Column(nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String fingerprint;

    /** If this finding was carried over from a previous version */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carried_from_id")
    private Finding carriedFrom;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    /** Optimistic lock version for concurrent modification detection */
    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Finding() {}

    public Finding(Contract contract, ContractVersion version, String ruleId,
                   Severity severity, String sectionLabel, String evidence,
                   String message, String fingerprint) {
        this.contract = contract;
        this.version = version;
        this.ruleId = ruleId;
        this.severity = severity;
        this.sectionLabel = sectionLabel;
        this.evidence = evidence;
        this.message = message;
        this.fingerprint = fingerprint;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public Contract getContract() { return contract; }
    public ContractVersion getVersion() { return version; }
    public String getRuleId() { return ruleId; }
    public FindingSource getSource() { return source; }
    public Severity getSeverity() { return severity; }
    public FindingStatus getStatus() { return status; }
    public boolean isNeedsCounsel() { return needsCounsel; }
    public ClauseEntity getClause() { return clause; }
    public String getSectionLabel() { return sectionLabel; }
    public String getEvidence() { return evidence; }
    public Integer getEvidenceStart() { return evidenceStart; }
    public Integer getEvidenceEnd() { return evidenceEnd; }
    public Integer getPage() { return page; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return details; }
    public BigDecimal getConfidence() { return confidence; }
    public String getFingerprint() { return fingerprint; }
    public Finding getCarriedFrom() { return carriedFrom; }
    public String getResolutionNote() { return resolutionNote; }
    public AppUser getAssignee() { return assignee; }
    public long getLockVersion() { return lockVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- Setters ---

    public void setSource(FindingSource source) { this.source = source; }
    public void setNeedsCounsel(boolean needsCounsel) { this.needsCounsel = needsCounsel; }
    public void setClause(ClauseEntity clause) { this.clause = clause; }
    public void setEvidenceStart(Integer evidenceStart) { this.evidenceStart = evidenceStart; }
    public void setEvidenceEnd(Integer evidenceEnd) { this.evidenceEnd = evidenceEnd; }
    public void setPage(Integer page) { this.page = page; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public void setCarriedFrom(Finding carriedFrom) { this.carriedFrom = carriedFrom; }
    public void setAssignee(AppUser assignee) { this.assignee = assignee; }

    /**
     * Transitions to a new status. Caller must validate the transition is legal.
     */
    public void transitionTo(FindingStatus newStatus, String resolutionNote) {
        this.status = newStatus;
        if (resolutionNote != null) {
            this.resolutionNote = resolutionNote;
        }
        this.updatedAt = Instant.now();
    }
}
