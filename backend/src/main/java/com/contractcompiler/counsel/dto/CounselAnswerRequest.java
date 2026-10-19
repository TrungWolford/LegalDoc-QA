package com.contractcompiler.counsel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for POST /public/counsel/{token}/answers.
 * Allows counsel to submit answers for one or more questions (partial answers allowed).
 */
public record CounselAnswerRequest(
        @NotEmpty(message = "At least one answer is required")
        @Valid
        List<Answer> answers
) {
    /**
     * An answer for a single question, identified by position.
     */
    public record Answer(
            /** Position of the question in the package (1-based) */
            int position,

            /** The selected answer option or free-text answer */
            String choice,

            /** Optional explanatory note */
            String note
    ) {}
}
