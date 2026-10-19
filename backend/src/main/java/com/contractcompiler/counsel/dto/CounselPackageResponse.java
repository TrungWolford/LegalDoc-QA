package com.contractcompiler.counsel.dto;

import java.util.List;

/**
 * Response body for GET /public/counsel/{token}.
 * Contains the question package as seen by counsel (external view, no internal IDs exposed).
 */
public record CounselPackageResponse(
        String title,
        String contractTitle,
        List<CounselQuestion> questions
) {
    /**
     * A question as presented to counsel, with relevant contract excerpt.
     */
    public record CounselQuestion(
            int position,
            String questionText,
            List<String> options,
            String sectionLabel,
            String evidence,
            String answerChoice,
            String answerNote,
            boolean answered
    ) {}
}
