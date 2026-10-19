package com.contractcompiler.analysis.ingest;

import com.contractcompiler.common.exception.AnalysisException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a DOCX file using Apache POI and returns a list of {@link RawLine}s.
 * Each paragraph becomes one RawLine. Page numbers are not available in DOCX
 * without rendering (Apache POI limitation), so {@code page} is null.
 */
@Component
public class DocxReader implements DocumentReader {

    @Override
    public List<String> supportedExtensions() {
        return List.of("docx");
    }

    @Override
    public List<RawLine> read(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(is)) {

            List<RawLine> lines = new ArrayList<>();
            int index = 0;

            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String text = paragraph.getText();
                if (text == null) text = "";
                // Include even blank lines so unparsed-line count is accurate
                String numbering = paragraph.getNumID() != null
                        ? paragraph.getNumID().toString() : null;
                lines.add(new RawLine(text, index++, null, numbering));
            }

            return lines;

        } catch (IOException e) {
            throw new AnalysisException("Cannot read DOCX file: " + e.getMessage(), e);
        }
    }
}
