package com.contractcompiler.analysis;

import com.contractcompiler.analysis.ingest.DocumentReader;
import com.contractcompiler.analysis.ingest.RawLine;
import com.contractcompiler.analysis.model.ContractModel;
import com.contractcompiler.analysis.model.CrossReference;
import com.contractcompiler.analysis.model.DefinedTerm;
import com.contractcompiler.analysis.parse.ClauseTreeBuilder;
import com.contractcompiler.analysis.parse.ClauseTreeBuilder.ClauseTreeBuildResult;
import com.contractcompiler.analysis.parse.ModelBuilder;
import com.contractcompiler.analysis.parse.ModelBuilder.ModelBuildResult;
import com.contractcompiler.analysis.persistence.*;
import com.contractcompiler.common.config.AppProperties;
import com.contractcompiler.common.exception.AnalysisException;
import com.contractcompiler.common.util.HashUtils;
import com.contractcompiler.contract.ContractVersion;
import com.contractcompiler.contract.ContractVersionRepository;
import com.contractcompiler.contract.VersionStatus;
import com.contractcompiler.contract.storage.FileStorage;
import com.contractcompiler.finding.Finding;
import com.contractcompiler.finding.FindingRepository;
import com.contractcompiler.finding.Severity;
import com.contractcompiler.rules.RuleEngine;
import com.contractcompiler.rules.RuleFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the full analysis pipeline for a contract version.
 *
 * <p>Called asynchronously after a version is saved. Steps:</p>
 * <ol>
 *   <li>Read file (DOCX or PDF) → raw lines</li>
 *   <li>Parse clause tree → {@link com.contractcompiler.analysis.parse.ClauseTree}</li>
 *   <li>Persist clauses to DB → get IDs</li>
 *   <li>Extract semantic entities (terms, refs, values)</li>
 *   <li>Persist entities to DB</li>
 *   <li>Build {@link ContractModel} → run all enabled rules</li>
 *   <li>Persist findings</li>
 *   <li>Mark version {@code REVIEW_READY} or {@code FAILED}</li>
 * </ol>
 */
@Service
public class AnalysisPipeline {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipeline.class);

    private final List<DocumentReader> readers;
    private final ClauseTreeBuilder clauseTreeBuilder;
    private final ModelBuilder modelBuilder;
    private final RuleEngine ruleEngine;
    private final FileStorage fileStorage;
    private final AppProperties appProperties;

    // Repositories
    private final ContractVersionRepository versionRepository;
    private final ClauseRepository clauseRepository;
    private final DefinedTermRepository definedTermRepository;
    private final CrossReferenceRepository crossReferenceRepository;
    private final FindingRepository findingRepository;

    public AnalysisPipeline(List<DocumentReader> readers,
                            ClauseTreeBuilder clauseTreeBuilder,
                            ModelBuilder modelBuilder,
                            RuleEngine ruleEngine,
                            FileStorage fileStorage,
                            AppProperties appProperties,
                            ContractVersionRepository versionRepository,
                            ClauseRepository clauseRepository,
                            DefinedTermRepository definedTermRepository,
                            CrossReferenceRepository crossReferenceRepository,
                            FindingRepository findingRepository) {
        this.readers = readers;
        this.clauseTreeBuilder = clauseTreeBuilder;
        this.modelBuilder = modelBuilder;
        this.ruleEngine = ruleEngine;
        this.fileStorage = fileStorage;
        this.appProperties = appProperties;
        this.versionRepository = versionRepository;
        this.clauseRepository = clauseRepository;
        this.definedTermRepository = definedTermRepository;
        this.crossReferenceRepository = crossReferenceRepository;
        this.findingRepository = findingRepository;
    }

    /**
     * Entry point — called asynchronously after upload.
     * Uses a dedicated thread pool defined in {@link com.contractcompiler.common.config.AsyncConfig}.
     *
     * @param versionId the ID of the freshly-saved ContractVersion
     */
    @Async("analysisExecutor")
    public void analyzeAsync(Long versionId) {
        log.info("[pipeline] Starting analysis for version {}", versionId);
        try {
            runPipeline(versionId);
        } catch (Exception e) {
            log.error("[pipeline] Analysis failed for version {}: {}", versionId, e.getMessage(), e);
            markFailed(versionId, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Pipeline steps
    // -------------------------------------------------------------------------

    @Transactional
    protected void runPipeline(Long versionId) {
        ContractVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new AnalysisException("Version not found: " + versionId));

        // --- Step 1: Resolve reader by file extension ---
        String ext = extractExtension(version.getFileName()).toLowerCase();
        DocumentReader reader = readers.stream()
                .filter(r -> r.supportedExtensions().contains(ext))
                .findFirst()
                .orElseThrow(() -> new AnalysisException("No reader for extension: " + ext));

        // --- Step 2: Read file ---
        Path filePath = fileStorage.resolve(version.getStoragePath());
        log.info("[pipeline] Reading file {} (ext={})", filePath.getFileName(), ext);
        List<RawLine> rawLines = reader.read(filePath);

        // Guard: page limit
        int maxPages = appProperties.analysis().maxPages();
        int maxParagraphs = appProperties.analysis().maxParagraphs();
        if (rawLines.size() > maxParagraphs) {
            throw new AnalysisException("Document exceeds maximum paragraph count: " + rawLines.size());
        }

        // --- Step 3: Parse clause tree ---
        log.info("[pipeline] Parsing clause tree ({} raw lines)", rawLines.size());
        ClauseTreeBuildResult buildResult = clauseTreeBuilder.build(rawLines);
        var tree = buildResult.tree();
        var report = buildResult.report();
        log.info("[pipeline] Parse complete: {} clauses, coverage={}%",
                tree.size(), String.format("%.1f", report.coveragePercent()));

        // Update page count from raw lines
        rawLines.stream()
                .filter(l -> l.page() != null)
                .mapToInt(RawLine::page)
                .max()
                .ifPresent(version::setPageCount);

        // --- Step 4: Persist clauses ---
        log.info("[pipeline] Persisting {} clause nodes", tree.allNodes().size());
        persistClauses(version, tree.roots(), null);

        // Reload clause map (label → entity) for linking
        Map<String, ClauseEntity> clauseByLabel = clauseRepository
                .findByVersionIdOrderBySeqAsc(versionId)
                .stream()
                .collect(Collectors.toMap(ClauseEntity::getLabel, Function.identity(),
                        (a, b) -> a)); // keep first on label collision

        // Backfill persistedId into ClauseNodes so ModelBuilder can use them
        for (var node : tree.allNodes()) {
            ClauseEntity entity = clauseByLabel.get(node.label());
            if (entity != null) node.setPersistedId(entity.getId());
        }

        // --- Step 5: Extract semantic entities ---
        log.info("[pipeline] Extracting semantic entities");
        ModelBuildResult extracted = modelBuilder.extract(tree);

        // Persist defined terms
        for (DefinedTerm dt : extracted.definedTerms()) {
            ClauseEntity defClause = clauseByLabel.get(dt.definitionClauseLabel());
            DefinedTermEntity entity = new DefinedTermEntity(
                    version, dt.term(), defClause,
                    dt.definitionText() != null ? dt.definitionText() : "");
            entity.setUsageCount(dt.usageCount());
            definedTermRepository.save(entity);
        }

        // Persist cross-references
        for (CrossReference ref : extracted.crossReferences()) {
            ClauseEntity sourceClause = clauseByLabel.get(ref.sourceClauseLabel());
            if (sourceClause == null) continue; // skip orphan refs
            CrossReferenceEntity entity = new CrossReferenceEntity(
                    version, sourceClause,
                    ref.rawText(), ref.targetLabel(),
                    ref.startOffset(), ref.endOffset());
            if (ref.resolved()) {
                ClauseEntity target = clauseByLabel.get(ref.targetLabel());
                if (target != null) entity.resolve(target);
            }
            crossReferenceRepository.save(entity);
        }

        // --- Step 6: Build ContractModel and run rule engine ---
        ContractModel model = new ContractModel(
                tree, report,
                extracted.definedTerms(),
                extracted.crossReferences(),
                extracted.extractedValues());

        log.info("[pipeline] Running rule engine");
        List<RuleFinding> ruleFindings = ruleEngine.analyze(model);
        log.info("[pipeline] Rule engine produced {} finding(s)", ruleFindings.size());

        // --- Step 7: Persist findings ---
        for (RuleFinding rf : ruleFindings) {
            String fingerprint = buildFingerprint(versionId, rf);
            Finding finding = new Finding(
                    version.getContract(), version,
                    rf.ruleId(), rf.severity(),
                    rf.sectionLabel(),
                    rf.evidence(),
                    rf.message(),
                    fingerprint);

            // Link to clause entity if available
            if (rf.clauseId() != null) {
                clauseRepository.findById(rf.clauseId())
                        .ifPresent(finding::setClause);
            }
            finding.setEvidenceStart(rf.evidenceStart());
            finding.setEvidenceEnd(rf.evidenceEnd());
            finding.setPage(rf.page());
            finding.setDetails(rf.details());
            finding.setConfidence(java.math.BigDecimal.valueOf(rf.confidence()));
            findingRepository.save(finding);
        }

        // --- Step 8: Mark version REVIEW_READY ---
        version.markReviewReady(report.unparsedLines());
        versionRepository.save(version);

        log.info("[pipeline] Version {} analysis complete. Status=REVIEW_READY, findings={}",
                versionId, ruleFindings.size());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Recursively persists ClauseNodes as ClauseEntity records (adjacency list).
     */
    private void persistClauses(ContractVersion version,
                                List<com.contractcompiler.analysis.parse.ClauseNode> nodes,
                                ClauseEntity parent) {
        for (var node : nodes) {
            ClauseEntity entity = new ClauseEntity(
                    version, parent,
                    (short) node.level(),
                    node.label(),
                    node.title() != null && !node.title().isBlank() ? node.title() : null,
                    node.body() != null ? node.body() : "",
                    node.seq(),
                    node.page(),
                    node.startOffset(),
                    node.endOffset());
            clauseRepository.save(entity);
            node.setPersistedId(entity.getId());

            if (!node.children().isEmpty()) {
                persistClauses(version, node.children(), entity);
            }
        }
    }

    private String buildFingerprint(Long versionId, RuleFinding rf) {
        String raw = versionId + "|" + rf.ruleId() + "|" + rf.sectionLabel()
                + "|" + rf.evidenceStart() + "|" + rf.evidenceEnd();
        return HashUtils.sha256(raw);
    }

    private void markFailed(Long versionId, String reason) {
        try {
            versionRepository.findById(versionId).ifPresent(v -> {
                v.markFailed(reason != null ? truncate(reason, 500) : "Unknown error");
                versionRepository.save(v);
            });
        } catch (Exception ex) {
            log.error("[pipeline] Could not mark version {} as FAILED: {}", versionId, ex.getMessage());
        }
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
