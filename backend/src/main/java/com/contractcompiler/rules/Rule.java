package com.contractcompiler.rules;

import com.contractcompiler.analysis.model.ContractModel;
import com.contractcompiler.finding.Severity;

import java.util.List;

/**
 * Interface for a single error-detection rule.
 *
 * <p>Each rule is a Spring {@code @Component} that receives an indexed {@link ContractModel}
 * and returns zero or more findings. Rules must:</p>
 * <ul>
 *   <li>Use the indexed model (maps by clause label, term, subject), not scan raw text</li>
 *   <li>Read thresholds and parameters from {@code rule_config.params}, never hard-code</li>
 *   <li>De-duplicate: one root cause = one finding (with occurrence count in details)</li>
 *   <li>Provide verbatim evidence with exact offsets</li>
 * </ul>
 *
 * <p>Adding a new rule requires only: one class implementing this interface, one {@code rule_config}
 * seed row, one question template entry, and one test class. Zero changes to the engine.</p>
 */
public interface Rule {

    /**
     * Stable rule identifier from {@link RuleIds}.
     */
    String id();

    /**
     * Default severity when not overridden by rule_config.
     */
    Severity defaultSeverity();

    /**
     * Analyze the contract model and return findings.
     *
     * @param model the fully indexed contract model (clause tree, terms, references, values)
     * @return list of findings (empty if no issues detected, never null)
     */
    List<RuleFinding> check(ContractModel model);
}
