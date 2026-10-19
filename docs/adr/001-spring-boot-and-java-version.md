# ADR-001: Spring Boot 3.4.5 + Java 21

## Status
Accepted (supersedes initial discussion about 4.1.1)

## Context
The system overview (doc 01) specifies Java 17 and Spring Boot 3. The project initializer created a
skeleton referencing Spring Boot 4.1.1 which is not a released version. Java 21 is the current LTS
and offers significant improvements.

## Decision
Use Spring Boot 3.4.5 (latest stable 3.x) + Java 21. Restructure the project to match doc 02 conventions:
- Package: `com.contractcompiler`
- Project root: `backend/`
- All architectural conventions from doc 02

## Consequences
- **Positive**: Stable, well-documented Spring Boot 3.x with Jakarta EE 10 namespace.
- **Positive**: Java 21 LTS with records, sealed classes, pattern matching.
- **Neutral**: Doc 01 says Java 17 but Java 21 is backward compatible.

## Date
2026-10-15
