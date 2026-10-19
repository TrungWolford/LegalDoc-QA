package com.contractcompiler.common.util;

/**
 * Text normalization utilities used across the analysis pipeline.
 * Handles Unicode normalization, smart quote conversion, and whitespace cleanup.
 */
public final class TextUtils {

    private TextUtils() {
        // utility class
    }

    /**
     * Normalizes smart quotes, em-dashes, and excess whitespace.
     * Preserves line breaks.
     */
    public static String normalizeUnicode(String text) {
        if (text == null) return "";
        return text
                // Smart quotes → straight quotes
                .replace('\u201C', '"')   // left double
                .replace('\u201D', '"')   // right double
                .replace('\u2018', '\'')  // left single
                .replace('\u2019', '\'')  // right single
                // Dashes
                .replace('\u2013', '-')   // en dash
                .replace('\u2014', '-')   // em dash
                // Non-breaking spaces
                .replace('\u00A0', ' ')
                // Multiple spaces to single
                .replaceAll("[ \\t]+", " ");
    }

    /**
     * Strips all whitespace and punctuation, lowercases — used for fingerprint normalization.
     */
    public static String normalizeForFingerprint(String text) {
        if (text == null) return "";
        return text
                .toLowerCase()
                .replaceAll("[\\s\\p{Punct}]+", "");
    }

    /**
     * Computes a finding fingerprint from rule ID, section label, and evidence.
     */
    public static String computeFingerprint(String ruleId, String sectionLabel, String evidence) {
        String normalized = ruleId + "|" + sectionLabel + "|" + normalizeForFingerprint(evidence);
        return HashUtils.sha256(normalized);
    }
}
