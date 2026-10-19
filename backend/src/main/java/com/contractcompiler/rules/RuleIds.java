package com.contractcompiler.rules;

/**
 * Stable rule IDs. Used in findings, fingerprints, rule_config table, and question templates.
 * Adding a new rule requires only adding a constant here and implementing the {@link Rule} interface.
 *
 * <p>Convention: UPPER_SNAKE_CASE, grouped by category prefix.</p>
 */
public final class RuleIds {

    private RuleIds() {}

    // --- Reference ---
    public static final String REF_BROKEN = "REF_BROKEN";
    public static final String REF_SELF = "REF_SELF";
    public static final String REF_TARGET_MISMATCH = "REF_TARGET_MISMATCH";
    public static final String REF_CIRCULAR = "REF_CIRCULAR";
    public static final String REF_RELATIVE_AMBIGUOUS = "REF_RELATIVE_AMBIGUOUS";

    // --- Definition ---
    public static final String TERM_UNDEFINED = "TERM_UNDEFINED";
    public static final String TERM_UNUSED = "TERM_UNUSED";
    public static final String TERM_DUPLICATE = "TERM_DUPLICATE";
    public static final String TERM_VARIANT = "TERM_VARIANT";
    public static final String TERM_CIRCULAR_DEF = "TERM_CIRCULAR_DEF";

    // --- Value ---
    public static final String DATE_LOGIC = "DATE_LOGIC";
    public static final String WORDS_VS_DIGITS = "WORDS_VS_DIGITS";
    public static final String VALUE_CONFLICT = "VALUE_CONFLICT";
    public static final String PERCENT_SUM = "PERCENT_SUM";
    public static final String CURRENCY_INCONSISTENT = "CURRENCY_INCONSISTENT";
    public static final String RANGE_INVALID = "RANGE_INVALID";
    public static final String DATE_FORMAT_INCONSISTENT = "DATE_FORMAT_INCONSISTENT";

    // --- Structure ---
    public static final String NUMBERING_DUPLICATE = "NUMBERING_DUPLICATE";
    public static final String NUMBERING_GAP = "NUMBERING_GAP";
    public static final String NUMBERING_ORDER = "NUMBERING_ORDER";
    public static final String SUBCLAUSE_ORPHAN = "SUBCLAUSE_ORPHAN";
    public static final String CLAUSE_EMPTY = "CLAUSE_EMPTY";
    public static final String TOC_MISMATCH = "TOC_MISMATCH";

    // --- Text quality ---
    public static final String PLACEHOLDER_LEFT = "PLACEHOLDER_LEFT";
    public static final String TEXT_REPEATED_WORD = "TEXT_REPEATED_WORD";
    public static final String TEXT_UNBALANCED = "TEXT_UNBALANCED";
    public static final String TEXT_TYPO = "TEXT_TYPO";

    // --- Entity ---
    public static final String PARTY_NAME_INCONSISTENT = "PARTY_NAME_INCONSISTENT";
    public static final String DUPLICATE_CLAUSE_TEXT = "DUPLICATE_CLAUSE_TEXT";

    // --- Meta ---
    public static final String PARSE_COVERAGE_LOW = "PARSE_COVERAGE_LOW";

    // --- AI ---
    public static final String AI_AMBIGUITY = "AI_AMBIGUITY";
}
