package com.contractcompiler.auth.dto;

import java.time.Instant;

/**
 * Response body for POST /api/auth/register.
 */
public record RegisterResponse(
        Long id,
        String email,
        String fullName,
        String role,
        Instant createdAt
) {}
