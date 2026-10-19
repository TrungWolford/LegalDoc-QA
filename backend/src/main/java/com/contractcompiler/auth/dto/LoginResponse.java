package com.contractcompiler.auth.dto;

/**
 * Response body for POST /api/auth/login.
 */
public record LoginResponse(
        String token,
        String email,
        String fullName,
        String role,
        long expiresIn
) {}
