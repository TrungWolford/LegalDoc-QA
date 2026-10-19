package com.contractcompiler.common.exception;

/**
 * Thrown when a finding status transition is not allowed by the state machine.
 */
public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(String fromStatus, String toStatus) {
        super("Invalid status transition: %s → %s".formatted(fromStatus, toStatus));
    }
}
