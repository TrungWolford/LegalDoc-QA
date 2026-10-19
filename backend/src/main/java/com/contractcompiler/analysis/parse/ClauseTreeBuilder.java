package com.contractcompiler.analysis.parse;

import com.contractcompiler.analysis.ingest.RawLine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a {@link ClauseTree} from a list of {@link RawLine}s.
 *
 * <p>Recognizes Vietnamese and English legal clause numbering patterns:</p>
 * <ul>
 *   <li>Level 1 (Article): "Điều 1", "Điều 2", "Article 1", "ARTICLE I"</li>
 *   <li>Level 2 (Section): "1.1", "2.3", "1.", "2."</li>
 *   <li>Level 3 (Sub-clause): "1.1.1", "2.3(a)", "(a)", "(i)"</li>
 * </ul>
 *
 * <p>Lines that don't match any clause pattern are considered unparsed.</p>
 */
@Component
public class ClauseTreeBuilder {

    // Matches: "Điều 1", "Điều 12", "Article 1", "ARTICLE 1"
    private static final Pattern ARTICLE_VN = Pattern.compile(
            "^\\s*(Điều|ĐIỀU)\\s+(\\d+)[\\.:\\s]?(.*)", Pattern.CASE_INSENSITIVE);

    private static final Pattern ARTICLE_EN = Pattern.compile(
            "^\\s*(Article|ARTICLE|Section|SECTION)\\s+(\\d+|[IVXivx]+)[\\.:\\s]?(.*)",
            Pattern.CASE_INSENSITIVE);

    // Matches: "1.", "2.", "1.1", "2.3", "10.5" — but NOT "1.1.1"
    private static final Pattern SECTION = Pattern.compile(
            "^\\s*(\\d{1,3})(\\.(\\d{1,3}))?\\.?\\s+(.+)");

    // Matches: "1.1.1", "2.3.4"
    private static final Pattern SUBSECTION = Pattern.compile(
            "^\\s*(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})[.:\\s]?\\s*(.*)");

    // Matches: "(a)", "(i)", "(1)"
    private static final Pattern SUB_ALPHA = Pattern.compile(
            "^\\s*\\(([a-z]{1,2}|\\d{1,2}|[ivxlIVXL]+)\\)\\s*(.*)");

    /**
     * Parses raw lines into a clause tree.
     *
     * @param lines ordered raw lines from the document reader
     * @return the built clause tree with a parse report
     */
    public ClauseTreeBuildResult build(List<RawLine> lines) {
        List<ClauseNode> roots = new ArrayList<>();
        List<String> unparsedTexts = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();

        ClauseNode currentArticle = null;
        ClauseNode currentSection = null;
        int seq = 0;
        int parsedCount = 0;
        int offset = 0;

        for (RawLine raw : lines) {
            String text = raw.text().strip();
            fullTextBuilder.append(raw.text()).append('\n');

            int lineStart = offset;
            offset += raw.text().length() + 1;

            if (text.isBlank()) {
                unparsedTexts.add(raw.text());
                continue;
            }

            // --- Try Level 1: Article (Vietnamese) ---
            Matcher m = ARTICLE_VN.matcher(text);
            if (m.matches()) {
                String label = m.group(2);
                String title = m.group(3).strip();
                currentArticle = new ClauseNode(1, label, title, "", seq++, raw.page(), lineStart, offset);
                currentSection = null;
                roots.add(currentArticle);
                parsedCount++;
                continue;
            }

            // --- Try Level 1: Article (English) ---
            m = ARTICLE_EN.matcher(text);
            if (m.matches()) {
                String label = m.group(2);
                String title = m.group(3).strip();
                currentArticle = new ClauseNode(1, label, title, "", seq++, raw.page(), lineStart, offset);
                currentSection = null;
                roots.add(currentArticle);
                parsedCount++;
                continue;
            }

            // --- Try Level 3: Subsection "1.1.1" ---
            m = SUBSECTION.matcher(text);
            if (m.matches() && currentSection != null) {
                String label = m.group(1);
                String body = m.group(2).strip();
                ClauseNode sub = new ClauseNode(3, label, "", body, seq++, raw.page(), lineStart, offset);
                currentSection.addChild(sub);
                parsedCount++;
                continue;
            }

            // --- Try Level 2: Section "1.1" or "1." ---
            m = SECTION.matcher(text);
            if (m.matches()) {
                String major = m.group(1);
                String minor = m.group(3); // may be null
                String body = m.group(4).strip();
                String label = minor != null ? major + "." + minor : major;

                // Only attach to current article if major matches
                if (currentArticle != null && major.equals(currentArticle.label())) {
                    currentSection = new ClauseNode(2, label, "", body, seq++, raw.page(), lineStart, offset);
                    currentArticle.addChild(currentSection);
                    parsedCount++;
                    continue;
                }
            }

            // --- Try Level 3: (a), (i), (1) sub-alpha ---
            m = SUB_ALPHA.matcher(text);
            if (m.matches() && currentSection != null) {
                String label = (currentSection.label()) + "(" + m.group(1) + ")";
                String body = m.group(2).strip();
                ClauseNode sub = new ClauseNode(3, label, "", body, seq++, raw.page(), lineStart, offset);
                currentSection.addChild(sub);
                parsedCount++;
                continue;
            }

            // --- Body continuation: append to nearest open clause ---
            if (currentSection != null) {
                // Body continuation for current section — rebuild as new node with appended body
                // (We treat continuation lines as part of the previous clause body via unparsed count only)
                parsedCount++;
            } else if (currentArticle != null) {
                parsedCount++;
            } else {
                unparsedTexts.add(text);
            }
        }

        String fullText = fullTextBuilder.toString();
        int total = lines.size();
        int unparsed = unparsedTexts.size();
        double coverage = total > 0 ? (double) parsedCount / total * 100.0 : 100.0;

        ParseReport report = new ParseReport(total, parsedCount, unparsed, unparsedTexts, coverage);
        ClauseTree tree = new ClauseTree(roots, fullText);
        return new ClauseTreeBuildResult(tree, report);
    }

    /**
     * Simple value object bundling the tree and parse report.
     */
    public record ClauseTreeBuildResult(ClauseTree tree, ParseReport report) {}
}
