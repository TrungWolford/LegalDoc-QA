package com.contractcompiler.rules;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the {@code rule_config} table.
 * Stores rule enablement, severity, and rule-specific parameters.
 */
@Entity
@Table(name = "rule_config")
public class RuleConfig {

    @Id
    @Column(name = "rule_id", length = 60)
    private String ruleId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "default_severity", nullable = false, length = 10)
    private String defaultSeverity;

    @Column(name = "severity_override", length = 10)
    private String severityOverride;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String params = "{}";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected RuleConfig() {}

    public String getRuleId() { return ruleId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public String getDefaultSeverity() { return defaultSeverity; }
    public String getSeverityOverride() { return severityOverride; }
    public String getParams() { return params; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Returns the effective severity: override if set, otherwise default.
     */
    public String getEffectiveSeverity() {
        return severityOverride != null ? severityOverride : defaultSeverity;
    }
}
