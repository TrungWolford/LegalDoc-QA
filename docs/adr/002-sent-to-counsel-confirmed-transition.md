# ADR-002: SENT_TO_COUNSEL → CONFIRMED transition

## Status
Accepted

## Context
The state machine diagram in doc 01 (Section 7, "Máy trạng thái của Finding") does not show the
`SENT_TO_COUNSEL → CONFIRMED` transition. However, doc 03 (Section 4, state transition table) explicitly
lists this transition with the note: "khi gói hết hạn, thu hồi" (when package expires, revoke).

The master build prompt instructs: "Treat doc 03 as correct."

## Decision
Implement the transition `SENT_TO_COUNSEL → CONFIRMED` as specified in doc 03. This transition is used
when a question package expires or is revoked, allowing the finding to return to the `CONFIRMED` state
for re-triaging.

## Consequences
- **Positive**: Reviewers can re-triage findings that were sent to counsel but never answered (expired packages).
- **Risk**: None. This is a natural recovery path.

## Date
2026-10-15
