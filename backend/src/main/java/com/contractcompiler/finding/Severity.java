package com.contractcompiler.finding;

/**
 * Severity levels for findings.
 * <ul>
 *   <li>{@code ERROR} — deterministic contradiction that must be resolved</li>
 *   <li>{@code WARNING} — heuristic/probable issue with evidence</li>
 *   <li>{@code INFO} — informational, low confidence or stylistic</li>
 * </ul>
 */
public enum Severity {
    ERROR,
    WARNING,
    INFO
}
