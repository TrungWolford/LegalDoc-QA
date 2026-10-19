package com.contractcompiler.common.exception;

/**
 * Thrown when analysis of a contract version fails (parse failure, file corruption, etc.).
 */
public class AnalysisException extends RuntimeException {

    public AnalysisException(String reason) {
        super(reason);
    }

    public AnalysisException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
