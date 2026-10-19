package com.contractcompiler.finding;

/**
 * Types of audit trail events recorded in {@code finding_event}.
 */
public enum EventAction {
    CREATED,
    STATUS_CHANGED,
    COMMENTED,
    ASSIGNED,
    SENT_TO_COUNSEL,
    COUNSEL_ANSWERED,
    CARRIED_OVER
}
