package com.contractcompiler.analysis.ingest;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for reading a document file into raw lines.
 * Implementations: {@code DocxReader} (Apache POI), {@code PdfReader} (Apache PDFBox).
 *
 * <p>The rest of the pipeline is format-agnostic; it only sees {@link RawLine}s.</p>
 */
public interface DocumentReader {

    /**
     * Returns the file extensions this reader supports (lowercase, without dot).
     */
    List<String> supportedExtensions();

    /**
     * Reads the file at the given path and returns raw lines.
     *
     * @param filePath path to the uploaded document
     * @return ordered list of raw lines with page and numbering info
     * @throws com.contractcompiler.common.exception.AnalysisException if the file cannot be read
     */
    List<RawLine> read(Path filePath);
}
