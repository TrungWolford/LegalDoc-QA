package com.contractcompiler.analysis.persistence;

import com.contractcompiler.analysis.model.ValueType;
import com.contractcompiler.contract.ContractVersion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity for the {@code extracted_value} table.
 * Represents a numeric value (money, percentage, date, or money-in-words)
 * extracted from a clause.
 */
@Entity
@Table(name = "extracted_value")
public class ExtractedValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ContractVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clause_id", nullable = false)
    private ClauseEntity clause;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ValueType valueType;

    /** Original text as it appears in the document */
    @Column(name = "raw_text", nullable = false, length = 300)
    private String rawText;

    /** Numeric representation for MONEY, PERCENT, MONEY_WORDS */
    @Column(name = "numeric_value", precision = 24, scale = 6)
    private BigDecimal numericValue;

    /** Date for DATE type values */
    @Column(name = "date_value")
    private LocalDate dateValue;

    /** ISO currency code (e.g. "USD") */
    @Column(length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    /** The financial term this value relates to (e.g. "Management Fee") */
    @Column(name = "subject_term", length = 200)
    private String subjectTerm;

    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    protected ExtractedValueEntity() {}

    public ExtractedValueEntity(ContractVersion version, ClauseEntity clause,
                                ValueType valueType, String rawText,
                                int startOffset, int endOffset) {
        this.version = version;
        this.clause = clause;
        this.valueType = valueType;
        this.rawText = rawText;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public ContractVersion getVersion() { return version; }
    public ClauseEntity getClause() { return clause; }
    public ValueType getValueType() { return valueType; }
    public String getRawText() { return rawText; }
    public BigDecimal getNumericValue() { return numericValue; }
    public LocalDate getDateValue() { return dateValue; }
    public String getCurrency() { return currency; }
    public String getSubjectTerm() { return subjectTerm; }
    public int getStartOffset() { return startOffset; }
    public int getEndOffset() { return endOffset; }

    // --- Setters ---

    public void setNumericValue(BigDecimal numericValue) { this.numericValue = numericValue; }
    public void setDateValue(LocalDate dateValue) { this.dateValue = dateValue; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setSubjectTerm(String subjectTerm) { this.subjectTerm = subjectTerm; }
}
