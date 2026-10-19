package com.contractcompiler.analysis.model;

/**
 * Types of values extracted from contract text.
 */
public enum ValueType {
    /** Monetary amount (e.g., "$1,000,000") */
    MONEY,
    /** Percentage (e.g., "2.5%") */
    PERCENT,
    /** Date (e.g., "January 15, 2025") */
    DATE,
    /** Monetary amount expressed in words (e.g., "One Hundred Thousand Dollars") */
    MONEY_WORDS
}
