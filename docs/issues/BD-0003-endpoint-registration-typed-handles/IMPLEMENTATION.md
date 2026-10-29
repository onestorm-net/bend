# Implementation Log

## 2026-10-29: Client registration methods, endpoint handles, ADR 0019 and 0011 updates

**Key Changes:**
- Created `client-api/src/main/java/net/onestorm/bend/client/api/Client.java`: `registerOutgoing` and `registerIncoming` taking `NamespacedId` (D1), `@throws NullPointerException`/`IllegalStateException` (D6), class Javadoc on builder creation, reconnects, bidirectional endpoints, no waiting for the server, and the constants usage example
- Created `EndpointHandle.java` (`getId()`, `unregister()` with D3's re-register and idempotency text), `IncomingEndpoint.java` and `OutgoingEndpoint.java` (empty, `@param <T>`, D2)
- Modified `EndpointCodec.java`, `EndpointHandler.java` and `package-info.java`: links to `Client#registerIncoming`/`registerOutgoing` and the handle types (BD-0002 follow-up)
- Modified `docs/decisions/0019-supported-ids-full-snapshot.md`: both directions count, three triggers, direction-mismatch in Does not decide (D4)
- Modified `docs/decisions/0011-protocol-exception.md` and root `CLAUDE.md` § API design decisions: `IllegalStateException` for a call the current state forbids (D6)

**Learnings:**
- JaCoCo leaves interfaces without default methods out of `jacocoTestReport.xml` entirely when no test loads them: only `NamespacedId.java` appears, so the gap check has nothing to inspect for these four types (BD-0002 saw them as empty `<sourcefile>` entries).
- `{@link}` to a generic method needs the erased parameter list (`Client#registerOutgoing(NamespacedId, EndpointCodec)`); javadoc resolved all of them without warnings.

**Deviations from plan:**
- None.

**Follow-ups noted but not done:**
- Carry the planned `NettyClientTest` names from PLAN.md's Security & Cost Surface Proof lines into the client-netty epic's issues (PLAN.md Risks).
- BD-0004's `send` Javadoc should link from `EndpointCodec#encode`'s "the send fails" wording.
- BD-0001 D2's context sentence ("BD-0003 registers by `String`") is now outdated; `of(String)` stays, so no code change.
