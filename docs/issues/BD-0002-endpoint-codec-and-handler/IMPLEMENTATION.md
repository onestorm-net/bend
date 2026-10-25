# Implementation Log

## 2026-10-25: EndpointCodec and EndpointHandler with their Javadoc contract

**Key Changes:**
- Created `client-api/src/main/java/net/onestorm/bend/client/api/EndpointCodec.java`: `decode(ByteBuffer)` and `encode(T)`, Javadoc carrying D1 to D4 (read-only view valid only during the call, untrusted bytes, big-endian, unread bytes ignored, handed-over encode buffer, non-null results, failure behaviour)
- Created `client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandler.java`: `@FunctionalInterface`, `handle(T)`, Javadoc for D4
- Modified `client-api/src/main/java/net/onestorm/bend/client/api/package-info.java`: links both interfaces as the types a user implements

**Learnings:**
- JDK 25's javadoc flags a missing type-parameter tag as `warning: no @param for <T>` without any doclint flag; checked by deleting `@param <T>` from `EndpointHandler` once (AC 5 can fail).
- JaCoCo lists interfaces without default methods as empty `<sourcefile>` entries (no lines, no counters), so they never show as a coverage gap.

**Deviations from plan:**
- None.

**Follow-ups noted but not done:**
- The "implementation reports the failure" promise (D4) needs a logging choice in `client-netty` (PLAN.md Risks).
- BD-0003 and BD-0004 should `{@link}` the send and endpoint handles from this Javadoc once they exist.
