package com.contractcompiler.rules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for rule configuration. Used by the rule engine to load
 * enabled/disabled state and parameters at analysis time.
 */
@Repository
public interface RuleConfigRepository extends JpaRepository<RuleConfig, String> {

    List<RuleConfig> findByEnabledTrue();
}
