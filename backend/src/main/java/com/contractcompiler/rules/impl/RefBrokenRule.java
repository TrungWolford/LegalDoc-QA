package com.contractcompiler.rules.impl;

import com.contractcompiler.analysis.model.ContractModel;
import com.contractcompiler.analysis.model.CrossReference;
import com.contractcompiler.finding.Severity;
import com.contractcompiler.rules.Rule;
import com.contractcompiler.rules.RuleFinding;
import com.contractcompiler.rules.RuleIds;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * REF_BROKEN — detects cross-references where the target clause label
 * does not exist anywhere in the clause tree.
 *
 * <p>Example: "as defined in Điều 8.1" but Điều 8.1 does not exist.</p>
 */
@Component
public class RefBrokenRule implements Rule {

    @Override
    public String id() {
        return RuleIds.REF_BROKEN;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.ERROR;
    }

    @Override
    public List<RuleFinding> check(ContractModel model) {
        return model.crossReferences().stream()
                .filter(ref -> !ref.resolved())
                .filter(ref -> !ref.targetLabel().equals(ref.sourceClauseLabel())) // skip self (handled by REF_SELF)
                .map(ref -> toFinding(ref, model.clauseTree().fullText()))
                .toList();
    }

    private RuleFinding toFinding(CrossReference ref, String fullText) {
        int safeEnd = Math.min(ref.endOffset(), fullText.length());
        int safeStart = Math.min(ref.startOffset(), safeEnd);
        String evidence = safeStart < safeEnd
                ? fullText.substring(safeStart, safeEnd)
                : ref.rawText();

        return new RuleFinding(
                RuleIds.REF_BROKEN,
                Severity.ERROR,
                ref.sourceClauseId(),
                ref.sourceClauseLabel(),
                evidence.isBlank() ? ref.rawText() : evidence,
                safeStart,
                safeEnd,
                null,
                "Cross-reference '" + ref.rawText() + "' in clause " + ref.sourceClauseLabel()
                        + " points to '" + ref.targetLabel() + "' which does not exist in the document.",
                Map.of("targetLabel", ref.targetLabel(), "rawText", ref.rawText()),
                1.0
        );
    }
}
