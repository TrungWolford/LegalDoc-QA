package com.contractcompiler.common.exception;

import java.time.Instant;

/**
 * Consistent error response body returned by all API error responses.
 *
 * @param code    machine-readable error code
 * @param message human-readable error message
 * @param details additional detail (nullable)
 * @param traceId correlation ID from MDC for log tracing
 */
public record ErrorResponse(
        String code,
        String message,
        Object details,
        String traceId,
        Instant timestamp
) {
    public ErrorResponse(String code, String message, Object details, String traceId) {
        this(code, message, details, traceId, Instant.now());
    }
}
