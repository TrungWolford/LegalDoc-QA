package com.contractcompiler.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed configuration properties for the application.
 * Bound from the {@code app} prefix in application.yml.
 * All limits, thresholds and operational parameters are externalized here.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        SecurityProperties security,
        UploadProperties upload,
        AnalysisProperties analysis,
        CounselProperties counsel,
        NotificationProperties notification,
        BenchmarkProperties benchmark,
        LoginProperties login
) {
    public record SecurityProperties(
            JwtProperties jwt,
            CorsProperties cors,
            int bcryptStrength
    ) {
        public record JwtProperties(String secret, long expirationMs) {}
        public record CorsProperties(List<String> allowedOrigins) {}
    }

    public record UploadProperties(
            String storageDir,
            int maxFileSizeMb,
            List<String> allowedExtensions,
            List<String> allowedMimeTypes
    ) {}

    public record AnalysisProperties(
            int maxPages,
            int maxParagraphs,
            int parseCoverageThreshold
    ) {}

    public record CounselProperties(
            int tokenTtlDays,
            int rateLimitPerMinute,
            int sendRateLimitPerHour
    ) {}

    public record NotificationProperties(
            String sendgridApiKey,
            String fromEmail,
            String fromName
    ) {}

    public record BenchmarkProperties(
            double minRecall,
            double minPrecision
    ) {}

    public record LoginProperties(
            int maxFailures,
            int lockoutMinutes
    ) {}
}
