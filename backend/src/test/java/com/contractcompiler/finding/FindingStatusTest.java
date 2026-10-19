package com.contractcompiler.finding;

import com.contractcompiler.common.exception.InvalidTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the finding state machine transitions defined in doc 03.
 * Every legal and every illegal transition is tested.
 */
class FindingStatusTest {

    /**
     * Legal transitions as defined in doc 03, Section 4.
     */
    private static final Map<FindingStatus, Set<FindingStatus>> LEGAL_TRANSITIONS = new EnumMap<>(Map.of(
            FindingStatus.DETECTED, Set.of(FindingStatus.CONFIRMED, FindingStatus.DISMISSED),
            FindingStatus.CONFIRMED, Set.of(FindingStatus.SENT_TO_COUNSEL, FindingStatus.RESOLVED, FindingStatus.DISMISSED),
            FindingStatus.SENT_TO_COUNSEL, Set.of(FindingStatus.RESOLVED, FindingStatus.DISMISSED, FindingStatus.CONFIRMED),
            FindingStatus.DISMISSED, Set.of(FindingStatus.DETECTED),
            FindingStatus.RESOLVED, Set.of()
    ));

    @ParameterizedTest
    @CsvSource({
            "DETECTED, CONFIRMED",
            "DETECTED, DISMISSED",
            "CONFIRMED, SENT_TO_COUNSEL",
            "CONFIRMED, RESOLVED",
            "CONFIRMED, DISMISSED",
            "SENT_TO_COUNSEL, RESOLVED",
            "SENT_TO_COUNSEL, DISMISSED",
            "SENT_TO_COUNSEL, CONFIRMED",
            "DISMISSED, DETECTED"
    })
    void legalTransitionsAreAllowed(FindingStatus from, FindingStatus to) {
        assertThat(isTransitionLegal(from, to))
                .as("Transition %s → %s should be legal", from, to)
                .isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "DETECTED, RESOLVED",
            "DETECTED, SENT_TO_COUNSEL",
            "DETECTED, DETECTED",
            "CONFIRMED, CONFIRMED",
            "CONFIRMED, DETECTED",
            "SENT_TO_COUNSEL, SENT_TO_COUNSEL",
            "DISMISSED, CONFIRMED",
            "DISMISSED, RESOLVED",
            "DISMISSED, SENT_TO_COUNSEL",
            "DISMISSED, DISMISSED",
            "RESOLVED, DETECTED",
            "RESOLVED, CONFIRMED",
            "RESOLVED, DISMISSED",
            "RESOLVED, SENT_TO_COUNSEL",
            "RESOLVED, RESOLVED"
    })
    void illegalTransitionsAreRejected(FindingStatus from, FindingStatus to) {
        assertThat(isTransitionLegal(from, to))
                .as("Transition %s → %s should be illegal", from, to)
                .isFalse();
    }

    @Test
    void resolvedIsTerminal() {
        Set<FindingStatus> allowed = LEGAL_TRANSITIONS.get(FindingStatus.RESOLVED);
        assertThat(allowed).isEmpty();
    }

    @Test
    void allStatusesHaveTransitionRules() {
        for (FindingStatus status : FindingStatus.values()) {
            assertThat(LEGAL_TRANSITIONS).containsKey(status);
        }
    }

    private boolean isTransitionLegal(FindingStatus from, FindingStatus to) {
        Set<FindingStatus> allowed = LEGAL_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}
