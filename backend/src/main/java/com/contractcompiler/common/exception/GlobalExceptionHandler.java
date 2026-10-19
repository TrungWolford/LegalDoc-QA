package com.contractcompiler.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler that produces a consistent {@link ErrorResponse} for all errors.
 * No stack traces are ever exposed to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_KEY = "traceId";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidTransitionException ex) {
        return buildResponse(HttpStatus.CONFLICT, "INVALID_TRANSITION", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_FILE", ex.getMessage(), null);
    }

    @ExceptionHandler(AnalysisException.class)
    public ResponseEntity<ErrorResponse> handleAnalysis(AnalysisException ex) {
        log.error("Analysis failed: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_FAILED", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fieldErrors);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return buildResponse(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified by another user. Please reload and try again.", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "File exceeds the maximum allowed upload size.", null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid email or password.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String code,
                                                         String message, Object details) {
        String traceId = MDC.get(TRACE_ID_KEY);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, details, traceId));
    }
}
