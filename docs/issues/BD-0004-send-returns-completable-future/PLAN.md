# BD-0004-send-returns-completable-future: `OutgoingEndpoint.send` returns `CompletableFuture<Void>`

**Date:** 2026-10-29
**Status:** Approved
**Issue:** [BD-0004](https://onestorm.kendo.dev/projects/1/issues/BD-0004)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0004-send-returns-completable-future/DECISIONS.md)

## Goal
A client author sends a message on an outgoing endpoint with one call that never throws for a
normal connection state, and can either ignore the outcome or handle success and failure through
the returned future.

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| Method | `CompletableFuture<Void> send(T message)` on `OutgoingEndpoint<T>`, the only member it adds (D1) | Issue AC 1; BD-0003 PLAN row "Handle children": `OutgoingEndpoint` exists so `send` can be added |
| What success means | The future completes normally once the message is written to the connection; not that the server received it, since protocol-v1.md has no per-message acknowledgement | Issue AC 1 |
| Unusable endpoint | Future fails with `EndpointUnavailableException`; the cases are listed in the Javadoc (D4) | Issue AC 2; protocol-v1.md § Endpoint list updates, § Endpoint Message |
| Network failure | Future fails with the original exception (`completeExceptionally(cause)`; `join()` wraps it in `CompletionException`); its type is not documented (D6) | Issue AC 2 |
| Caller bugs | `send(null)` throws `NullPointerException`; a send after this handle's `unregister()` throws `IllegalStateException` (D3) | BD-0003 D6 and CLAUDE.md § API design decisions; `Client#registerOutgoing` `@throws` wording |
| Codec failure | `EndpointCodec#encode` throwing fails the future with that exception; returning `null` fails it with `NullPointerException` (D3) | BD-0002 issue AC "a failed encode fails the send's future"; `EndpointCodec#encode` Javadoc |
| Completion | The future completes when the message is written or the send fails, at the latest when the connection closes; `send` does not wait for the write; completing or cancelling the future does not affect the send (D1, D4) | Issue Context "callers can attach callbacks or ignore it" |
| Threads and ordering | Not stated: no promise about which thread runs `encode` or callbacks, nor about the order of writes | BD-0006 owns the threading contract; developer: no promises BD-0006 would have to correct |
| Exception type | `public class EndpointUnavailableException extends RuntimeException`, one public constructor `(String message)` (D2) | `ProtocolException` is the repo's other exception class; unchecked like it. Stack trace kept: ADR 0011's no-stack-trace rule binds only `ProtocolException`, and this is not a wire-input failure; at most one is created per `send` call, so a peer can't make the client build them faster than the caller sends |
| Built-in packets | Javadoc states `send` carries only this endpoint's messages; built-in packets never go through it | ADR 0008 Decision; issue AC 4 |
| Tests | No test class in `client-api` (D5) | BD-0003 D5, BD-0002 D7 |

**Layer compliance:** both changes live in `client-api` and use only `java.util.concurrent.CompletableFuture`,
`java.lang.RuntimeException` and types in `net.onestorm.bend.client.api`. `client-api/build.gradle.kts`
declares only `testImplementation(libs.assertj.core)`, and `client-api/CLAUDE.md` requires it to depend on
nothing, so this fits. No Lombok.

**Module shape:**

| Class | Inputs/outputs | Hidden behaviour | Test seam | Shallow-test reckoning | Verdict |
|---|---|---|---|---|---|
| `OutgoingEndpoint<T>` | `send(T)` → `CompletableFuture<Void>`, plus `getId()`/`unregister()` from `EndpointHandle` | In `client-netty`: encoding, the availability/direction/ack-cycle check, wrapping in `Endpoint Message`, the write and completing the future from its `ChannelFuture` | `client-netty`: send on an available endpoint and see the bytes and a completed future; send on an unavailable one and see `EndpointUnavailableException` | "Send and wait", "send with timeout" and "send several and wait for all" are composed on the returned future (`join`, `orTimeout`, `allOf`), not new methods. A send option (priority, say) would be a parameter. Only a different outcome type (an acknowledged send) would need a method, and the spec has no acknowledgement | Deep: one call hides the whole outbound path |
| `EndpointUnavailableException` | `(String message)` | None | None | Exempt: exception class (module-shape lens § Exempt) | Exempt |

## Scope
- **In scope:**
  - `client-api/src/main/java/net/onestorm/bend/client/api/OutgoingEndpoint.java`: `send` and its Javadoc
  - `client-api/src/main/java/net/onestorm/bend/client/api/EndpointUnavailableException.java` (new)
  - `client-api/src/main/java/net/onestorm/bend/client/api/EndpointCodec.java`: `encode` Javadoc links to `OutgoingEndpoint#send`
  - `client-api/src/main/java/net/onestorm/bend/client/api/Client.java`: class Javadoc example shows a `send`
  - `client-api/src/main/java/net/onestorm/bend/client/api/package-info.java`: names `send` and the exception
  - `client-api/CLAUDE.md`: one rule line (Documentation Sync)
  - Kendo BD-0004: acceptance criteria per D3 and D4
- **Out of scope:**
  - Queueing messages while disconnected (issue Scope)
  - Which thread runs `encode` and future callbacks, thread safety of `send`, and write ordering (BD-0006)
  - `close()`, connection state and its events (BD-0005)
  - The `client-netty` implementation and its tests
  - Outbound backpressure or a bound on queued writes
  - JSpecify `@NullMarked` on `client-api` (a later issue)
  - Any change to `protocol` or `protocol/protocol-v1.md`
  - A test class in `client-api` (D5)

## Approach

1. **`EndpointUnavailableException.java`** (new): `public class EndpointUnavailableException extends RuntimeException`.
   Class Javadoc: the exception a send's future fails with when the endpoint can't be used for that message;
   it reports a normal connection state, not a bug, which is why `send` never throws it (link
   `{@link OutgoingEndpoint#send(Object)}`). One constructor `public EndpointUnavailableException(String message)`
   calling `super(message)`, with `@param message` saying it names the endpoint and why it is unavailable.
2. **`OutgoingEndpoint.java`**: add `CompletableFuture<Void> send(T message)`. Javadoc, in this order:
   - Sends one message on this endpoint: the codec encodes it and the payload goes to the server in an
     `Endpoint Message`. Only this endpoint's messages are sent this way.
   - Returns without waiting for the write. The returned future completes normally once the message has been
     written to the connection. That does not mean the server received or handled it: the protocol has no
     per-message acknowledgement.
   - The caller may ignore the future (fire-and-forget; a failure is then dropped) or attach callbacks to it.
     The future completes once the message is written or the send fails; a write the connection does not take
     stays pending until the connection closes, and then fails. Completing or cancelling it does not affect
     the send.
   - The future fails with `{@link EndpointUnavailableException}` when this endpoint can't be used for the
     message: the server does not currently offer its id, the server's flags for it don't allow the client to
     send on it, the server has not yet acknowledged the client declaring it, the client is disconnected or
     closed, or this handle is unregistered before the message is written.
   - If the codec's `encode` throws, the future fails with that exception; if it returns `null`, with a
     `NullPointerException`. Nothing is written in either case.
   - If writing fails, the future fails with the exception that caused it; its type is not specified (D6).
   - `@param message the message to send`; `@return a future that completes once the message is written`;
     `@throws NullPointerException if {@code message} is {@code null}`; `@throws IllegalStateException if this
     handle has been unregistered`.
   - No statement about threads or the order of writes (BD-0006).
   Class Javadoc gains one sentence: messages are sent with `{@link #send(Object)}`.
3. **`EndpointCodec.java`**: in `encode`'s Javadoc, "If this method throws or returns `null`, the send fails
   and nothing is written" becomes "... the future returned by `{@link OutgoingEndpoint#send(Object)}` fails
   and nothing is written".
4. **`Client.java`**: the class Javadoc example gains `chat.send(new ChatMessage("hello"));` after the
   `registerOutgoing` line.
5. **`package-info.java`**: one sentence: messages are sent with `OutgoingEndpoint#send`, which reports
   failures through its future, an `EndpointUnavailableException` when the endpoint can't be used.
6. **`client-api/CLAUDE.md`**: add "`send` reports endpoint and connection state through its future
   (`EndpointUnavailableException`) and throws only for caller bugs. (BD-0004 D3, D4,
   [DECISIONS.md](/docs/issues/BD-0004-send-returns-completable-future/DECISIONS.md))".
7. **Kendo BD-0004**: acceptance criteria gain D3's `NullPointerException` and encode-failure rules, D4's
   list of unavailable cases and completion by connection close, and D6's unspecified cause type (done during
   planning).
8. Run `./gradlew :bend-client-api:javadoc`, `./gradlew :bend-client-api:dependencies --configuration compileClasspath`
   and `./gradlew build`.

## Security & Cost Surface

### Untrusted wire input and allocation bounds
N/A: nothing is read off the wire; `send` is outbound only and this branch adds no reader, writer or limit.
The one size hand-off is for `client-netty`: a payload from `encode` larger than the frame cap is rejected by
the outbound frame encoder, and that write failure must reach the caller as a failed future (the "writing
fails" clause), not as a disconnect or an unobserved exception. Proof: N/A: no code; planned in the
client-netty epic as `NettyClientTest#send_payloadAboveFrameCap_failsFuture`.

### Connection lifecycle, timeouts and silent peers
No state, wait or disconnect path is added; the contract fixes what a caller observes in each existing state.
Disconnected or closed: the future fails with `EndpointUnavailableException` (D4), and nothing is queued
(queueing is out of scope). Which exception a drop produces depends on where the send is: if the event-loop
check finds the connection gone, `EndpointUnavailableException`; once the write has been issued, the write's
cause (D6), including for a write that stays pending until the connection closes. Connection drops, or the
server half-closes, after the write was issued: the write's `ChannelFuture` fails and the future fails with that cause (verify at implementation
that Netty 4.2.13.Final fails pending writes when the channel closes, and how a half-close surfaces with the
default `ALLOW_HALF_CLOSURE=false`). A local `close()` (BD-0005) racing an accepted send that is not yet written
fails it with `EndpointUnavailableException`. A server whose socket stops draining leaves the write, and so the
future, pending. A cooperating server ends that: the client's `Keep Alive` echo is stuck behind the same write,
so the server's 30-second echo check (protocol-v1.md § Keep Alive: "Client must respond within 30 seconds ...
Failure results in disconnect") closes the connection, which fails the write. A hostile server that keeps
sending `Keep Alive` but never reads and never closes can hold the future open indefinitely: the client's own
condition (§ Client-side disconnect conditions, "No `Keep Alive` is received from the server within 30
seconds") never fires. So the contract promises completion by the time the connection closes, not within a
fixed time (D4); a client-side write timeout is a client-netty decision, listed under Risks. Proof: planned in
the client-netty epic, `NettyClientTest#send_whileDisconnected_failsWithEndpointUnavailable` and
`NettyClientTest#send_connectionClosedBeforeWrite_failsFutureWithCause`.

### Threading and the event loop
Deferred to BD-0006, not N/A: `send` is a public method callable from any thread, and `encode` and the future's
callbacks run somewhere. This branch promises nothing about which thread, about thread safety of `send`, or about
the order of writes, so BD-0006 adds those rules without contradicting any Javadoc here. The one thing stated
is that `send` does not wait for the write; `encode` may still run on the caller's thread, and nothing here
says otherwise. The one ordering obligation this branch hands `client-netty` is internal, not a Javadoc
promise: the availability check and the write of an `Endpoint Message`, and the writing of an updated
`Supported Endpoints`, run on the channel's event loop, so a check and its write can't straddle a list update
(Spec conformance, Runtime add and remove). Making the `IllegalStateException` after `unregister()` visible to a
`send` on another thread (`volatile` or loop confinement) is BD-0006's. Unaddressed hazard, handed to BD-0006:
the future is completed from the write listener on the event loop, so `whenComplete` callbacks may run there,
and `send(x).join()` called on the event loop (for example from a handler, if BD-0006 runs handlers there)
would deadlock (Risks). Proof: none possible in this branch, since no thread rule is added; BD-0006 names its
own tests, and the event-loop obligation is proved by
`NettyClientTest#send_acceptedThenUnregistered_notWrittenAfterUpdatedList`, which must run on a real event loop
(`DefaultEventLoop` or `NioEventLoopGroup`) with `send` and `unregister` called from a non-loop thread, not on
an `EmbeddedChannel`.

### Resource ownership
No buffer or future is created in `client-api`. The contract hands two obligations to `client-netty`: the
buffer `encode` returns is owned by Bend from then on (`EndpointCodec#encode` Javadoc, BD-0002) and is
converted to a `ByteBuf` there by wrapping, not copying, which that hand-over makes safe (verify at
implementation); and every `writeAndFlush` gets a listener that completes or fails the returned
future, since an unobserved failed write would leave the future pending. On a failed write the `ByteBuf` made
from the payload is released by Netty (verify at implementation which call in Netty 4.2.13.Final releases a
message whose write fails or whose channel is already closed); when `encode` throws or returns `null`, no
`ByteBuf` is made, so nothing is released. Per-connection growth: accepted sends wait either in the event
loop's task queue (check not yet run) or in Netty's outbound buffer (written, not flushed to the socket); no
separate pending-send set is kept, because the check and the write happen in one event-loop task. Neither
queue is bounded by this branch: a fire-and-forget loop on a live, slow connection grows them, and this is
accepted explicitly for now (Risks; backpressure is out of scope). While disconnected, sends fail at once, so
nothing accumulates. Proof: planned in the client-netty epic, `NettyClientTest#send_writeFails_futureFailsWithCause`
and `NettyClientTest#send_writeFails_releasesPayload` (asserts `refCnt() == 0`, run with `ResourceLeakDetector`
at `PARANOID`).

### Spec conformance
Touches protocol-v1.md § Endpoint Message (client » server): "An endpoint message can only be sent if the server
has the endpoint available and the client has declared it supported. The endpoint's flags determine the valid
direction." § Endpoint list updates: "New endpoints in an updated list MUST NOT be used until the full
acknowledge cycle for that list is complete" and "Before sending the updated list ..., the sender MUST stop
sending `Endpoint Message`s on any removed endpoints." D4 turns each into an `EndpointUnavailableException` on
the client, so `send` never writes a message the client knows is forbidden, and the server's disconnect
conditions for an unsupported or wrong-direction message (§ Disconnect conditions) are not triggered by such a
send. Enforcement is `client-netty`'s, checked on the event loop in the same task that writes the message. A
server-side removal takes effect when the client processes the new `Available Endpoints`, before it sends the
acknowledgement (§ Available Endpoints Acknowledged: "Removed endpoints MUST NOT be used by the client after
this"); a message written before the client received that list is the spec's in-flight window, which the
server handles. No spec edit, no version change. Proof:
planned in the client-netty epic, `NettyClientTest#send_beforeSupportedAcknowledged_failsWithEndpointUnavailable`
and `NettyClientTest#send_serverOffersServerboundForbidden_failsWithEndpointUnavailable`.

### Public API surface, module boundaries and enforcement
Adds `OutgoingEndpoint#send(T)` returning `java.util.concurrent.CompletableFuture<Void>`, and the public class
`EndpointUnavailableException`, both with Javadoc. No Netty type in either signature, enforced at L1:
`client-api/build.gradle.kts` has no main dependency, so an `io.netty` import cannot compile; no module gains a
dependency. A network failure's cause may at runtime be a Netty subclass of a JDK exception (verify at
implementation which class Netty 4.2.13.Final uses for a write on a closed channel); ADR 0012 binds signatures,
and callers catch it by its JDK supertype. Caller misuse: `send(null)` is `NullPointerException` and a send after
`unregister()` is `IllegalStateException`, both L3 checks in the implementation, specified here with `@throws`.
ADR 0008's split: `send` takes only `T` of its own endpoint, so a built-in packet cannot be passed in its type
(L1 for this caller); keeping `client-netty`'s internal path off the endpoint path is L4 review, as ADR 0008
already chose. The new `client-api/CLAUDE.md` rule ("`send` reports state through its future and throws only
for caller bugs") is L4 in this branch (Javadoc and the rule line); it becomes L2 once the planned client-netty
tests exist, which are not written yet. Trade-off: until then a `client-netty` change that throws from `send`
for a state case escapes everything but review; accepted because `client-api` has no implementation to test.
Likewise the `NullPointerException` and `IllegalStateException` rules are contract text only: nothing in this
branch enforces them, and they become L3 checks in `client-netty`. The failure cause's type is unspecified (D6), so no caller can rely on
a Netty or `protocol` class through it. Proof: `./gradlew :bend-client-api:dependencies --configuration compileClasspath` prints "No
dependencies"; L3 checks planned as `NettyClientTest#send_afterUnregister_throwsIllegalState` and
`NettyClientTest#send_null_throwsNullPointer`.

### Runtime endpoint add and remove
`send` is where the add/remove cycle becomes visible to a caller. Add: after `registerOutgoing` or after a new
`Available Endpoints` that offers the id, sends fail with `EndpointUnavailableException` until the server has
acknowledged the `Supported Endpoints` revision that declares the id; a stale acknowledgement of an older
revision does not make it usable (§ Revisions). Remove, local: after this handle's `unregister()`, a new send
throws `IllegalStateException`; a send accepted before it is either written before the updated `Supported
Endpoints` goes out or fails with `EndpointUnavailableException`, never written after it. This rests on the
event-loop serialization point named under Threading: `unregister` removes the id from the usable set in the
same event-loop task that writes the new list, so a later check fails. Overlapping updates: a send accepted
while revision N declared the id, when revision N+1 without the id has already been written but not yet
acknowledged, fails, because the id left the usable set when N+1 was written, not when it is acknowledged.
Remove, server side: a new `Available Endpoints` without the id makes later sends, and sends not yet written,
fail with `EndpointUnavailableException` from the moment the client processes it; the registration stays, so
sending works again once a later list offers the id and the cycle completes. Unregistering the incoming half of a bidirectional pair does not affect `send`.
Proof: planned in the client-netty epic, `NettyClientTest#send_acceptedThenUnregistered_notWrittenAfterUpdatedList`
and `NettyClientTest#send_idRemovedByServer_failsWithEndpointUnavailable`.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | `OutgoingEndpoint<T>` declares exactly `CompletableFuture<Void> send(T message)` | Read `OutgoingEndpoint.java`; `./gradlew :bend-client-api:compileJava` |
| 2 | `send`'s Javadoc states: success means written, not received, as the protocol has no per-message acknowledgement; `send` does not wait; the future completes when written or failed, at the latest when the connection closes; completing or cancelling it does not affect the send | Read `OutgoingEndpoint.java` |
| 3 | `send`'s Javadoc lists every `EndpointUnavailableException` case of D4, the encode-failure rules of D3, and that a write failure fails the future with its cause, type unspecified (D6) | Read `OutgoingEndpoint.java` against DECISIONS.md D3, D4, D6 |
| 3a | `send`'s Javadoc says only this endpoint's messages are sent through it, and `OutgoingEndpoint`'s class Javadoc says messages are sent with `{@link #send(Object)}` | Read `OutgoingEndpoint.java` |
| 4 | `send` has `@throws NullPointerException` (null message) and `@throws IllegalStateException` (unregistered handle), and its Javadoc says nothing about threads or write order | Read `OutgoingEndpoint.java` |
| 5 | `EndpointUnavailableException` is public, extends `RuntimeException`, and has exactly one public constructor taking a `String` | Read `EndpointUnavailableException.java` |
| 6 | `EndpointCodec#encode` says a throw or `null` fails the future returned by `{@link OutgoingEndpoint#send(Object)}`; the `Client` class example calls `send` on the registered handle; `package-info.java` names `OutgoingEndpoint#send` and `EndpointUnavailableException` | Read the three files |
| 7 | Every public type and member has Javadoc and every `{@link}` resolves | `./gradlew :bend-client-api:javadoc` prints no `warning:` lines |
| 8 | `client-api` still has no main dependency | `./gradlew :bend-client-api:dependencies --configuration compileClasspath` prints "No dependencies" |
| 9 | `client-api/CLAUDE.md` has the `send` rule line linking BD-0004 DECISIONS.md | Read `client-api/CLAUDE.md` |
| 10 | The build passes | `./gradlew build` |
| 11 | Kendo BD-0004's acceptance criteria carry D3, D4 and D6 (done during planning) | Read the issue via `mcp__kendo__prepare-issue-context-tool` |

## Reuse and patterns

- `client-api/src/main/java/net/onestorm/bend/client/api/Client.java`: mirror: `@throws NullPointerException if ... is {@code null}` and `@throws IllegalStateException if ...` wording; edit in place for the example
- `client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandle.java`: mirror: contract-in-Javadoc voice of `unregister`; call: `unregister()` is what makes `send` throw
- `client-api/src/main/java/net/onestorm/bend/client/api/EndpointCodec.java`: call: `encode` is what `send` invokes; edit in place for the link
- `protocol/src/main/java/net/onestorm/bend/protocol/exception/ProtocolException.java`: mirror: an unchecked exception class with a message constructor (without its stack-trace switch, see Key Design Decisions)

## Testing Strategy

No test class in this PR (D5): `send` is an interface method and `EndpointUnavailableException` has a single
constructor that only calls `super(message)`, so no test here could fail for a change to the contract. The
behaviour is tested where it is implemented, in the client-netty epic; the planned cases are the Proof lines of
the Security & Cost Surface, each with its red case:

| Planned test (client-netty) | Fails when |
|---|---|
| `NettyClientTest#send_availableEndpoint_writesEndpointMessageAndCompletes` | the future completes before the write, or never |
| `NettyClientTest#send_whileDisconnected_failsWithEndpointUnavailable` | `send` throws or the future stays pending |
| `NettyClientTest#send_beforeSupportedAcknowledged_failsWithEndpointUnavailable` | a message is written before the acknowledge cycle completes |
| `NettyClientTest#send_afterUnregister_throwsIllegalState` | the call returns a future instead of throwing |
| `NettyClientTest#send_encodeThrows_failsFutureWithThatException` | the codec's exception escapes `send` or is replaced |
| `NettyClientTest#send_writeFails_futureFailsWithCause` | the write failure is not observed or loses its cause |
| `NettyClientTest#send_writeFails_releasesPayload` | the payload `ByteBuf` is left with `refCnt() > 0` after a failed write |

Verification for this PR: `./gradlew :bend-client-api:javadoc` (AC 7; the task does not fail on warnings, so
read its output), `./gradlew :bend-client-api:dependencies --configuration compileClasspath` (AC 8), then
`./gradlew build`.

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: no wire or rule change; D4 only makes the client honour the existing § Endpoint Message and § Endpoint list updates rules |
| New or changed public type or method in `client-api` | Javadoc on that type | Update: `OutgoingEndpoint#send`, `EndpointUnavailableException`; links in `EndpointCodec#encode`, `Client`, `package-info.java` |
| A design decision, layer rule, convention or module plan | `CLAUDE.md` / ADR | Update: `client-api/CLAUDE.md` gains the `send` line (D3, D4), so BD-0005's "a send after `close()` fails its future" follows the same rule. No ADR changes: ADR 0011's Decision already covers `IllegalStateException` for a state-forbidden call |

## Edge Cases
- `send(null)`: `NullPointerException` at the call (D3).
- Send after this handle's `unregister()`: `IllegalStateException` at the call; a send accepted before the unregister is written or fails with `EndpointUnavailableException` (D4).
- `encode` throws: the future fails with that exception; `encode` returns `null`: it fails with `NullPointerException`; nothing is written (D3).
- Id offered only server to client: the future fails with `EndpointUnavailableException` (D4).
- Registered but the `Supported Endpoints` cycle is not acknowledged yet: `EndpointUnavailableException` (D4).
- Disconnected or closed: `EndpointUnavailableException`, nothing queued.
- Connection drops or the server half-closes during the write: the future fails with the write's cause, type unspecified (D6).
- Server stops reading but keeps sending `Keep Alive`: the future stays pending until the connection closes (D4).
- Send accepted under revision N, revision N+1 without the id already written: fails with `EndpointUnavailableException`.
- Caller cancels or completes the returned future: the send goes ahead unaffected (D1).

## Risks
- **Unbounded outbound buffer.** A fire-and-forget loop on a live but slow connection keeps writing; nothing in this contract pushes back. Accepted for now; backpressure would be its own issue.
- **Hostile server holds writes open.** A server that keeps sending `Keep Alive` but stops reading never trips either side's spec timeout, so a future can stay pending for the life of the connection. A client-side write timeout is a client-netty decision.
- **Netty or `protocol` exception classes as causes.** A write failure's cause can be a Netty subclass of a JDK exception, or `ProtocolException` for a payload above the frame cap. D6 leaves the type unspecified, so callers that match on the exact class are outside the contract.
- **Blocking on the future from the event loop.** Future callbacks run on whichever thread completes the future, likely the event loop, and `join()` called there deadlocks. Nothing here warns callers, because threading is BD-0006's; BD-0006 must address it.
- **Pending after close.** "Completes by the time the connection closes" depends on `client-netty` attaching a listener to every write and failing accepted-but-unwritten sends on unregister, disconnect and close; a missed path is a silent hang for careful callers.
- **Shape may change after the prototype.** D1 is to be revisited once a first working prototype exists; changing the return type then breaks callers, which is acceptable in 0.x (ADR 0020).
