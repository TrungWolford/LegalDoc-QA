package com.contractcompiler.analysis.ingest;

/**
 * A single raw line extracted from a document before normalization.
 *
 * @param text      the raw text content
 * @param lineIndex zero-based index in the document
 * @param page      page number (1-based, nullable for DOCX)
 * @param numbering original numbering from the document format (e.g., DOCX paragraph numbering), nullable
 */
public record RawLine(
        String text,
        int lineIndex,
        Integer page,
        String numbering
) {
    public RawLine {
        if (text == null) throw new IllegalArgumentException("text must not be null");
    }
}
