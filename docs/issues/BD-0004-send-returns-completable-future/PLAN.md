# BD-0004-send-returns-completable-future: `OutgoingEndpoint.send`

**Date:** 2026-11-05
**Status:** Approved
**Issue:** [BD-0004](https://onestorm.kendo.dev/projects/1/issues/BD-0004)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0004-send-returns-completable-future/DECISIONS.md)

The branch name predates D1: it keeps "completable-future" because PR #7 stays open on it.

## Goal
A client author sends a message on an outgoing endpoint with one call that fails at once, with a
clear exception, for anything the client itself can tell is wrong.

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| Method | `void send(T message)` on `OutgoingEndpoint<T>` (D1) | BD-0003 PLAN: `OutgoingEndpoint` exists to carry `send` |
| Refused states | Plain `IllegalStateException`, message naming the reason, for unregistered, not offered, flags, not acknowledged, disconnected, closed; all listed in the Javadoc (D2) | ADR 0011 Decision (state-forbidden call), BD-0003 D6; protocol-v1.md § Flow, § Endpoint list updates, § Endpoint Message |
| Null message | `send(null)` throws `NullPointerException` (D2) | `Client#registerOutgoing` `@throws` wording; BD-0003 D6 |
| After `send` returns | An accepted message may still be dropped, without a report; the Javadoc's reasons are examples, not a closed list (D3) | Issue scope: lifecycle is BD-0005 |
| Codec and frame-cap failures | Not in this contract; BD-0006 decides when `encode` runs (D4) | BD-0006 owns threads |
| Built-in packets | Javadoc: `send` carries only this endpoint's messages | ADR 0008 Decision |

**Layer compliance:** all changes are in `client-api`, using only `java.lang` and
`net.onestorm.bend.client.api` types; `client-api/build.gradle.kts` declares only
`testImplementation(libs.assertj.core)`. No Lombok.

**Module shape:** `OutgoingEndpoint` is deep: one call hides encoding, the state checks, the
`Endpoint Message` wrapping and the write. The second use case Risks names (a
non-throwing variant, or an `isAvailable()` query) would be a method, but it comes from the
exception policy (D1), not from splitting `send` per use case, so the verdict stands.

## Scope
- **In scope:** `OutgoingEndpoint.java` (`send` signature and Javadoc), removing the branch's
  `EndpointUnavailableException.java` (D2), `package-info.java`,
  `EndpointCodec.java` (`encode` failure sentence), `client-api/CLAUDE.md` (rule line),
  Kendo BD-0004 title and acceptance criteria.
- **Out of scope:** when and on which thread `encode` runs, codec failures and the frame-cap check
  (BD-0006, D4); `close()`, connection state and its events (BD-0005); an `isAvailable()` query
  (D1); an exception subtype for refused states (D2); queueing while disconnected; the `client-netty` implementation and its tests; any change to
  `protocol` or `protocol-v1.md`.

## Approach

All in `client-api/src/main/java/net/onestorm/bend/client/api/`, on top of the branch as it is:

1. `EndpointUnavailableException.java`: delete (D2).
2. `OutgoingEndpoint.java`: `void send(T message)`; drop the `CompletableFuture` import. Javadoc:
   - Sends one message: the codec encodes it, the payload goes to the server in an
     `Endpoint Message`. Only this endpoint's messages travel this way.
   - Returning means the message was accepted, not that it was written or that the server received
     it; the protocol has no per-message acknowledgement. An accepted message may still be dropped,
     for example when the endpoint or the connection stops being usable before it is written, or
     when the write fails; `send` does not report it (D3).
   - `@throws NullPointerException` if `message` is `null`.
   - `@throws IllegalStateException` if this handle has been unregistered, the server does not
     currently offer this id, the server's flags for it don't allow the client to send on it, the
     server has not yet acknowledged the `Supported Endpoints` that declares it, the client is
     disconnected, or the client is closed; the message names the reason.
3. `package-info.java`: messages are sent with `send`, which throws an `IllegalStateException`
   when the endpoint can't be used right now.
4. `EndpointCodec.java`: the `encode` sentence becomes "If this method throws or returns
   `{@code null}`, the message passed to `{@link OutgoingEndpoint#send(Object)}` is not sent." It
   links `send` as BD-0003's plan expected, and says nothing about how that is reported (D4).
5. `client-api/CLAUDE.md`: replace the `send` line (Documentation Sync).
6. Delete `IMPLEMENTATION.md`: it logs the future-based version and would mislead `/implement-plan`.

`Client.java`'s example `chat.send(new ChatMessage("hello"));` stays as is.

## Security & Cost Surface

### Untrusted wire input and allocation bounds
N/A: the branch reads nothing off the wire; it changes a signature and Javadoc in `client-api`.

### Connection lifecycle, timeouts and silent peers
`send` waits for nothing, so it adds no wait to bound. Disconnected and closed throw
`IllegalStateException` at the call (D2); a disconnect after the check drops the message
without a report (D3), and the caller learns of the connection through BD-0005. No `Disconnect` is
sent by `send`. Proof: the `@throws` and drop sentences in `OutgoingEndpoint#send` Javadoc.

### Threading and the event loop
With nothing returned there is no handle to wait on, but the interface alone does not make `send`
non-blocking: BD-0006 must keep it so, which client-netty verifies at implementation time. The
state checks read state the event loop changes, so BD-0006 also owns making them safe from any
thread, along with which thread runs `encode` and write order (D4). The Javadoc promises none of
them. Proof: no thread wording in the `send` Javadoc.

### Resource ownership
No buffer, future or channel crosses the API; the payload's `ByteBuffer` ownership is already stated
on `EndpointCodec#encode` and is unchanged. Two things the plan leaves open on purpose: whether the
caller may modify the message after `send` returns depends on when `encode` runs, so BD-0006 decides
it; and accepted-but-unwritten messages need a bound per connection (Netty's write-buffer water
marks, verify at implementation; D3 lets `send` drop at that bound) and a write listener that feeds a failed write into BD-0005's
connection state, both client-netty's. Proof: both listed under Risks.

### Spec conformance
`send` refuses what protocol-v1.md forbids the client to send: § Endpoint Message ("only ... if the
server has the endpoint available and the client has declared it supported. The endpoint's flags
determine the valid direction") and § Endpoint list updates ("New endpoints ... MUST NOT be used
until the full acknowledge cycle for that list is complete"). No spec change. Proof: the
`IllegalStateException` cases in the Javadoc map one to one onto those rules.

### Public API surface, module boundaries and enforcement
One new method (`send`, returning `void`) and no new type; no Netty or
`java.util.concurrent` type in a signature, no new dependency. `send` stays the endpoint-message
path only (ADR 0008), enforced L1: the signature takes a `T` for this endpoint's codec, not a
packet. Caller misuse throws per D2. The new `client-api/CLAUDE.md` rule (which states throw) is
L4 here, Javadoc and review only, and becomes L2 with the client-netty epic's contract tests. A
caller catching `IllegalStateException` around `send` also catches its own unregistered-handle bug;
D2 accepts that until a caller needs a subtype. `send` carries Javadoc (ADR 0021).
Proof: `./gradlew :bend-client-api:javadoc --rerun` without warnings checks the links only; the rule
itself is checked by reading the Javadoc against D2.

### Runtime endpoint add and remove
An id the server removed throws once the client knows it is gone; an id just added throws until the
client's `Supported Endpoints` for it is acknowledged. A removal that lands between the check and
the write drops the message (D3): § Endpoint Message only allows a send while "the server has the
endpoint available", which the implementation re-checks at write time. The client's own removal
(`unregister()` and the `Supported Endpoints` resend of ADR 0019) is bound by § Endpoint list
updates ("Before sending the updated list ... the sender MUST stop sending `Endpoint Message`s on
any removed endpoints"), so client-netty stops writing on that id before it resends the list. Proof: the
"not yet acknowledged" case and the drop sentence in the Javadoc.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | `OutgoingEndpoint#send` is `void send(T message)` and `client-api` has no `CompletableFuture` | `grep -rn CompletableFuture client-api/src` finds nothing; `./gradlew :bend-client-api:build` passes |
| 2 | `client-api` has no `EndpointUnavailableException` | `grep -rn EndpointUnavailable client-api/src client-api/CLAUDE.md` finds nothing |
| 3 | The `send` Javadoc has `@throws NullPointerException` for `null` and `@throws IllegalStateException` for the six cases of D2, says returning is not delivery, says an accepted message may still be dropped (examples, not a closed list), and says nothing about threads, codec failures or the frame cap | read the Javadoc against D2 to D4 |
| 4 | `client-api/CLAUDE.md`'s `send` line matches D1 and D2 and links this DECISIONS.md | read the line |
| 5 | Javadoc builds cleanly | `./gradlew :bend-client-api:javadoc --rerun` shows no javadoc warning |
| 6 | No future wording is left in `client-api` (`package-info` and `EndpointCodec#encode` included) | `grep -rni future client-api/src` finds nothing |
| 7 | `IMPLEMENTATION.md` from the future-based version is gone | `ls docs/issues/BD-0004-send-returns-completable-future/` lists only `PLAN.md` and `DECISIONS.md` until `/implement-plan` writes a new log |
| 8 | Kendo BD-0004's title and acceptance criteria describe `void send` and D2 to D4 | read the issue with `prepare-issue-context-tool` |

## Reuse and patterns

- `client-api/src/main/java/net/onestorm/bend/client/api/Client.java`: mirror: the `@throws`
  wording of `registerOutgoing` for `NullPointerException` and `IllegalStateException`.
- `client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandle.java`: mirror: how
  `unregister` Javadoc words what happens after the call.

## Testing Strategy

No test class in `client-api`: the branch adds no executable logic, only a signature, Javadoc and the
removal of an exception class, so no test could fail for a change to the contract (BD-0003 D5). The behaviour,
including each `IllegalStateException` case and the drop after `send` returns, is tested in
the `client-netty` epic. Checks here: `./gradlew :bend-client-api:build` and
`./gradlew :bend-client-api:javadoc --rerun`.

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: no wire change |
| New or changed public type or method in `client-api` | Javadoc on that type | Update: `OutgoingEndpoint#send`, `package-info`, `EndpointCodec#encode` |
| A design decision, layer rule, convention or module plan | `CLAUDE.md` | Update: `client-api/CLAUDE.md` line becomes "`send` returns nothing and throws a plain `IllegalStateException` for every state the client can check that forbids the send; no exception subtype until a caller needs one. (BD-0004 D1, D2, DECISIONS.md link)". Root `CLAUDE.md` and ADR 0011: N/A, a plain `IllegalStateException` for a call the current state forbids is already their rule |

## Edge Cases

- Send while disconnected and reconnecting: `IllegalStateException`; the caller decides to drop
  or retry (D2).
- Send on an outgoing registration whose id the server flags clientbound only: BD-0003 allows the
  registration, `send` throws `IllegalStateException` (D2).
- Send right after registering, before the server acknowledged the new `Supported Endpoints`:
  `IllegalStateException` (D2, spec § Endpoint list updates).
- Disconnect between the check and the write: the message may be dropped, no report (D3).
- Server removes the endpoint after `send` accepted a message: client-netty may write it before
  acknowledging the removal, or drop it; the contract allows both (D3).
- Send on this handle after its `unregister()`, even if the id was registered again:
  `IllegalStateException`; the new handle is unaffected (D2, `EndpointHandle#unregister`).

## Risks

- Fire-and-forget callers now need a `try`/`catch` around every `send` while the client reconnects;
  if that proves noisy, an `isAvailable()` query or a non-throwing variant is the follow-up (D1).
  A caller that must tell its own lifecycle bugs from connection states needs a subtype, which can
  be added later without breaking callers (D2).
- BD-0006's description still says "future callbacks run on the I/O thread" and doesn't yet carry
  D4's codec and frame-cap items; it needs updating before it is planned. It must also decide
  whether the caller may modify a message after `send` returns, keep `send` non-blocking, and make
  the state checks safe from any thread.
- With no per-message report, a failed write or an unbounded backlog of accepted messages is only
  visible if client-netty adds a write listener that feeds BD-0005's connection state and bounds
  pending writes (Netty's write-buffer water marks, verify at implementation).
