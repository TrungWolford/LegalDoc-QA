package com.contractcompiler.common.exception;

/**
 * Thrown when file upload validation fails (bad extension, MIME type, magic bytes, or size).
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String reason) {
        super("Invalid file: " + reason);
    }
}
