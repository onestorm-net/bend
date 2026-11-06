# Implementation Log

## 2026-11-05: `send` returns `void` and throws (replan)

**Key Changes:**
- Modified `client-api/src/main/java/net/onestorm/bend/client/api/OutgoingEndpoint.java`: `void send(T message)` with the D1 to D3 contract
- Modified `client-api/src/main/java/net/onestorm/bend/client/api/EndpointUnavailableException.java`: extends `IllegalStateException` (D2)
- Modified `EndpointCodec.java` (`encode` failure sentence without a future, D4) and `package-info.java`
- Modified `client-api/CLAUDE.md`: the `send` rule line
- Replaced the log of the future-based version; it lives in git history (commit `c2aeca5`)

**Learnings:**
- `EndpointUnavailableException`'s constructor stays the one missed line in `client-api`'s JaCoCo report; it is reachable (client-netty will throw it) and untested on purpose (BD-0003 D5).

**Deviations from plan:**
- None.

**Follow-ups noted but not done:**
- BD-0006's description needs the D4 items before it is planned (PLAN.md Risks).
- client-netty epic: a write listener feeding BD-0005's connection state, and a bound on pending writes (PLAN.md Risks).

## 2026-11-06: review pass; plain `IllegalStateException`, open-ended drop wording

**Key Changes:**
- Deleted `client-api/src/main/java/net/onestorm/bend/client/api/EndpointUnavailableException.java` (D2 revised: no subtype until a caller needs one)
- Modified `OutgoingEndpoint.java`: one `@throws IllegalStateException` for all six refused states; the drop sentence gives examples, not a closed list (D3 revised)
- Modified `package-info.java`, `client-api/CLAUDE.md`, PLAN.md and DECISIONS.md to match

**Learnings:**
- The earlier entry's JaCoCo note no longer applies: `client-api` has no untested executable line left.
- ADR 0011 allows splitting exception types only when a caller must catch them apart; adding an `IllegalStateException` subclass later is compatible, removing one is not, which carried the revision.

**Deviations from plan:**
- None; the plan was revised with the developer before the change.

**Follow-ups noted but not done:**
- `.claude/agents/runtime-integrity-reviewer.md` and `precedent-reviewer.md` don't load as agent types (filed as a hive finding).
