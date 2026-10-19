# Sample Contracts

This directory contains sample contract documents for testing.

## Files

| File | Description |
|---|---|
| `lpa_clean.docx` | Clean LPA with no errors — regression gate |
| `lpa_with_errors_v1.docx` | LPA v1 with intentionally seeded errors |
| `lpa_with_errors_v2.docx` | LPA v2 with some errors fixed, some new |

## Note

These files are **generated programmatically** by test fixtures using Apache POI.
See `backend/src/test/java/com/contractcompiler/benchmark/` for the fixture builders.

Hand-written realistic LPA excerpts are kept in `backend/src/test/resources/contracts/`.
