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
