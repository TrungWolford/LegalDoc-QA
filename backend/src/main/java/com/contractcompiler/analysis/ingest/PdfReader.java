package com.contractcompiler.analysis.ingest;

import com.contractcompiler.common.exception.AnalysisException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a PDF file using Apache PDFBox 3.x and returns a list of {@link RawLine}s.
 * Each line of text becomes one RawLine with its page number.
 *
 * <p>PDFBox 3.x API: {@code Loader.loadPDF(File)} replaces the deprecated
 * {@code PDDocument.load(File)}.</p>
 */
@Component
public class PdfReader implements DocumentReader {

    @Override
    public List<String> supportedExtensions() {
        return List.of("pdf");
    }

    @Override
    public List<RawLine> read(Path filePath) {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {

            List<RawLine> lines = new ArrayList<>();
            int globalIndex = 0;
            int totalPages = document.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                for (String line : pageText.split("\\r?\\n")) {
                    lines.add(new RawLine(line, globalIndex++, page, null));
                }
            }

            return lines;

        } catch (IOException e) {
            throw new AnalysisException("Cannot read PDF file: " + e.getMessage(), e);
        }
    }
}
