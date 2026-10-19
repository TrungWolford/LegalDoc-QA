package com.contractcompiler.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/contracts (multipart, this is the metadata part).
 * The file itself is sent as a multipart file parameter.
 */
public record CreateContractRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title
) {}
