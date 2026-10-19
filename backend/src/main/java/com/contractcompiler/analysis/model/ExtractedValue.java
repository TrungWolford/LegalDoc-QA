package com.contractcompiler.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A typed value extracted from the contract (money, percentage, date, or number-in-words).
 *
 * @param valueType    type of the extracted value
 * @param rawText      the verbatim text (e.g., "$1,000,000", "2.5%", "January 15, 2025")
 * @param numericValue parsed numeric value (for MONEY, PERCENT, MONEY_WORDS)
 * @param dateValue    parsed date (for DATE)
 * @param currency     ISO 4217 currency code (for MONEY, MONEY_WORDS)
 * @param subjectTerm  the associated subject/term (e.g., "Management Fee")
 * @param clauseLabel  the clause containing this value
 * @param clauseId     persisted clause ID (nullable before persistence)
 * @param startOffset  start offset in full text
 * @param endOffset    end offset in full text
 */
public record ExtractedValue(
        ValueType valueType,
        String rawText,
        BigDecimal numericValue,
        LocalDate dateValue,
        String currency,
        String subjectTerm,
        String clauseLabel,
        Long clauseId,
        int startOffset,
        int endOffset
) {}
