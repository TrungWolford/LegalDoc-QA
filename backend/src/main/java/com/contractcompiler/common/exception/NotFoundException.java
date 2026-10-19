package com.contractcompiler.common.exception;

/**
 * Thrown when a requested resource is not found in the database.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resourceType, Object id) {
        super("%s not found: %s".formatted(resourceType, id));
    }
}
