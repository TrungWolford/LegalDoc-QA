# ADR-003: Extended rule categories in rule_config

## Status
Accepted

## Context
Doc 03's `rule_config` table CHECK constraint lists categories: `REFERENCE`, `DEFINITION`, `VALUE`,
`STRUCTURE`, `AI`. However, the master build prompt's detection catalog includes rules in categories
`TEXT_QUALITY`, `ENTITY`, and `META` (e.g., `PLACEHOLDER_LEFT`, `PARTY_NAME_INCONSISTENT`,
`PARSE_COVERAGE_LOW`).

## Decision
Extended the CHECK constraint in V5 migration to include all categories from the detection catalog:
`REFERENCE`, `DEFINITION`, `VALUE`, `STRUCTURE`, `TEXT_QUALITY`, `ENTITY`, `META`, `AI`.

Seeded all 31 rules from the detection catalog (P0, P1, P2) with appropriate `enabled` flags:
- P0/P1 rules: `enabled = TRUE`
- P2 rules: `enabled = FALSE`

## Consequences
- **Positive**: All rules can be configured from day one, even before implementation.
- **Positive**: The seed data serves as documentation of the full rule catalog.

## Date
2026-10-15
