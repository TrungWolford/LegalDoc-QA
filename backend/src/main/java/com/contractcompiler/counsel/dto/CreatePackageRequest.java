package com.contractcompiler.counsel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for POST /api/packages.
 * Creates a new counsel question package from a selection of findings.
 */
public record CreatePackageRequest(
        @NotEmpty(message = "At least one finding ID is required")
        List<Long> findingIds,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Counsel email is required")
        @Email(message = "Invalid email format")
        String counselEmail,

        @Size(max = 150, message = "Counsel name must not exceed 150 characters")
        String counselName
) {}
