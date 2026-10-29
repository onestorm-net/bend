# Implementation Log

## 2026-10-29: `send` contract and `EndpointUnavailableException`

**Key Changes:**
- Created `client-api/src/main/java/net/onestorm/bend/client/api/EndpointUnavailableException.java`: unchecked, one `(String message)` constructor (D2)
- Modified `client-api/src/main/java/net/onestorm/bend/client/api/OutgoingEndpoint.java`: `CompletableFuture<Void> send(T message)` with the D1, D3, D4 and D6 contract; class Javadoc links `send`
- Modified `EndpointCodec.java` (`encode` links `OutgoingEndpoint#send`), `Client.java` (example calls `send`), `package-info.java` (names `send` and the exception)
- Modified `client-api/CLAUDE.md`: the `send` rule line

**Learnings:**
- `EndpointUnavailableException`'s constructor is the only executable line in `client-api` without a test; it is reachable (`client-netty` will call it) and left untested on purpose per D5, so the JaCoCo report shows it missed until the client-netty epic exercises it.
- `./gradlew :bend-client-api:javadoc --rerun` prints only Gradle's own native-access warnings, none from javadoc, so every `{@link}` resolves.

**Deviations from plan:**
- None.

**Follow-ups noted but not done:**
- Kendo issue: adopt JSpecify `@NullMarked` on `client-api` (developer asked to draft it later).
- BD-0006 must address callbacks and `join()` on the event loop, and the visibility of the unregistered state across threads (PLAN.md Risks).
- client-netty epic: write timeout and outbound backpressure decisions; the planned `NettyClientTest` cases in PLAN.md's Testing Strategy.
