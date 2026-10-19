package com.contractcompiler.rules;

import com.contractcompiler.analysis.model.ContractModel;
import com.contractcompiler.finding.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates all enabled {@link Rule} implementations against a {@link ContractModel}.
 *
 * <p>At analysis time:</p>
 * <ol>
 *   <li>Loads enabled rule configs from DB</li>
 *   <li>Filters the injected Rule beans to only enabled ones</li>
 *   <li>Runs each rule, collecting findings</li>
 *   <li>Applies severity overrides from config</li>
 * </ol>
 *
 * <p>Adding a new rule requires zero changes here — just a new {@code @Component}
 * implementing {@link Rule} and a {@code rule_config} row.</p>
 */
@Service
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<Rule> allRules;
    private final RuleConfigRepository ruleConfigRepository;

    public RuleEngine(List<Rule> allRules, RuleConfigRepository ruleConfigRepository) {
        this.allRules = allRules;
        this.ruleConfigRepository = ruleConfigRepository;
    }

    /**
     * Runs all enabled rules against the contract model.
     *
     * @param model the fully indexed contract model
     * @return all findings from all enabled rules
     */
    public List<RuleFinding> analyze(ContractModel model) {
        Map<String, RuleConfig> enabledConfigs = ruleConfigRepository.findByEnabledTrue()
                .stream()
                .collect(Collectors.toMap(RuleConfig::getRuleId, rc -> rc));

        List<RuleFinding> allFindings = new ArrayList<>();

        for (Rule rule : allRules) {
            RuleConfig config = enabledConfigs.get(rule.id());
            if (config == null) {
                log.debug("Rule {} is disabled or not configured, skipping", rule.id());
                continue;
            }

            try {
                log.info("Running rule: {}", rule.id());
                List<RuleFinding> findings = rule.check(model);

                // Apply severity override from config
                Severity overrideSeverity = parseSeverity(config.getEffectiveSeverity());
                if (overrideSeverity != null && overrideSeverity != rule.defaultSeverity()) {
                    findings = findings.stream()
                            .map(f -> new RuleFinding(
                                    f.ruleId(), overrideSeverity, f.clauseId(), f.sectionLabel(),
                                    f.evidence(), f.evidenceStart(), f.evidenceEnd(), f.page(),
                                    f.message(), f.details(), f.confidence()
                            ))
                            .toList();
                }

                log.info("Rule {} produced {} finding(s)", rule.id(), findings.size());
                allFindings.addAll(findings);
            } catch (Exception e) {
                log.error("Rule {} failed: {}", rule.id(), e.getMessage(), e);
                // A failing rule should not block the entire analysis
            }
        }

        return allFindings;
    }

    private Severity parseSeverity(String value) {
        try {
            return value != null ? Severity.valueOf(value) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
