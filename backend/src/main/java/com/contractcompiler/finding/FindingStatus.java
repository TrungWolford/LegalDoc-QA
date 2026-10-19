package com.contractcompiler.finding;

/**
 * Finding status in the workflow state machine.
 * Transitions are enforced by {@link FindingWorkflowService}, not by the database.
 *
 * <pre>
 * DETECTED → CONFIRMED, DISMISSED
 * CONFIRMED → SENT_TO_COUNSEL, RESOLVED, DISMISSED
 * SENT_TO_COUNSEL → RESOLVED, DISMISSED, CONFIRMED (revoke on expiry)
 * DISMISSED → DETECTED (reviewer changed mind)
 * RESOLVED → (terminal)
 * </pre>
 */
public enum FindingStatus {
    DETECTED,
    CONFIRMED,
    SENT_TO_COUNSEL,
    RESOLVED,
    DISMISSED
}
