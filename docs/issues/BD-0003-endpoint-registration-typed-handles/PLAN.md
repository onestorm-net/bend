# BD-0003-endpoint-registration-typed-handles: endpoint registration with typed handles

**Date:** 2026-10-29
**Status:** Approved
**Issue:** [BD-0003](https://onestorm.kendo.dev/projects/1/issues/BD-0003)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0003-endpoint-registration-typed-handles/DECISIONS.md)

## Goal
A client author registers an incoming or outgoing endpoint by its `NamespacedId` and gets a typed
handle back that identifies that registration and can unregister it, so a later `send` goes to
the endpoint the caller chose instead of one looked up by payload class.

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| Where registration lives | New `public interface Client` in `client-api` with only `registerOutgoing` and `registerIncoming`; no `connect()`, no settings | ADR 0018 Decision; `client-api/CLAUDE.md`; BD-0005 and BD-0004 add lifecycle and `send` later |
| Outgoing signature | `<T> OutgoingEndpoint<T> registerOutgoing(NamespacedId id, EndpointCodec<T> codec)` | Issue AC 1, with `NamespacedId` instead of `String` (D1) |
| Incoming signature | `<T> IncomingEndpoint<T> registerIncoming(NamespacedId id, EndpointCodec<T> codec, EndpointHandler<T> handler)` | Issue AC 1, D1 |
| Id type | `NamespacedId` only; validity is guaranteed by `NamespacedId.of` (D1) | `NamespacedId#of` throws `IllegalArgumentException` (BD-0001 D6) |
| Handle parent | `public interface EndpointHandle` with `NamespacedId getId()` and `void unregister()`; non-generic (D2) | Issue AC 2; BD-0001 D1 chose `get` accessors to match `getId()` |
| Handle children | `public interface IncomingEndpoint<T> extends EndpointHandle {}` and `public interface OutgoingEndpoint<T> extends EndpointHandle {}`, both empty | Issue AC 2: own types so methods can be added without breaking binary compatibility; BD-0004 adds `send` to `OutgoingEndpoint` |
| Duplicate registration | Same id twice in the same direction: `IllegalStateException`; one incoming plus one outgoing with the same id is a bidirectional endpoint | Issue AC 3 and Context. Deliberate refinement of CLAUDE.md § API design decisions / ADR 0011, which name only `IllegalArgumentException`: D6 adds `IllegalStateException` for state conflicts and updates both |
| `null` arguments | `NullPointerException`, documented with `@throws` | `NamespacedId#of` Javadoc (BD-0001 D6) |
| Unregister | Can be called more than once; later calls do nothing. The id may be registered again afterwards, returning a new handle (D3) | Issue AC 4; ADR 0016 (runtime add/remove is core) |
| Reconnects | Registrations belong to the `Client` and survive reconnects; Javadoc says so | Issue AC 5 |
| Supported set | ADR 0019 updated: both directions count, local unregister is a trigger (D4) | protocol-v1.md § Endpoint message routing |
| Package | `net.onestorm.bend.client.api`, next to `NamespacedId`, `EndpointCodec`, `EndpointHandler` | BD-0002 D5; layout review is BD-0013 |
| Javadoc | Every type and member, `@param <T>` on generic types and methods, `@throws` for `NullPointerException`/`IllegalStateException` | `client-api/CLAUDE.md` (ADR 0021); `NamespacedId.java` style |
| Tests | No test class (D5) | BD-0002 D7 |

**Layer compliance:** all four types live in `client-api` and refer only to `NamespacedId`,
`EndpointCodec` and `EndpointHandler` in the same package. `client-api/build.gradle.kts` declares
only `testImplementation(libs.assertj.core)`, and CLAUDE.md § API design decisions requires
`client-api` to depend on nothing, so this fits. No Lombok (BD-0001 D8).

**Module shape:**

| Class | Inputs/outputs | Hidden behaviour | Test seam | Shallow-test reckoning | Verdict |
|---|---|---|---|---|---|
| `Client` | `registerOutgoing(id, codec)` → `OutgoingEndpoint<T>`; `registerIncoming(id, codec, handler)` → `IncomingEndpoint<T>` | In `client-netty`: the registration table, matching against the server's available list, the supported-set snapshot and resend (ADR 0019), keeping registrations across reconnects | `client-netty`: register, receive `Available Endpoints`, see `Supported Endpoints`; register twice and get `IllegalStateException` | A new endpoint is a new call with another id/codec (a parameter). A new direction-free kind of endpoint would be a method, but the spec has exactly two directions. Lifecycle (BD-0005) adds methods for a different concern, not per endpoint | Deep: two primitives hide the whole advertise/withdraw cycle |
| `EndpointHandle` | `getId()`, `unregister()` | In `client-netty`: withdrawing the id from the supported set when no registration for it remains, idempotency | `client-netty`: unregister the last registration for an available id and see a new `Supported Endpoints` without it | A second handle kind (another direction) reuses both methods unchanged; a new per-handle capability (availability check, out of scope) is one method shared by all handles, not one per use case | Deep for its size: one call hides the withdraw cycle |
| `IncomingEndpoint<T>` / `OutgoingEndpoint<T>` | Nothing beyond `EndpointHandle` today; `send(T)` lands on `OutgoingEndpoint` in BD-0004 | The type parameter ties a handle to its codec's message type | Compile-time only: `registerOutgoing(id, EndpointCodec<String>)` yields `OutgoingEndpoint<String>` | They exist so methods can be added without breaking binary compatibility (issue AC 2); each future method serves every endpoint of that direction. `T` is declared now because adding it later breaks callers' source (D2) | Shallow-but-justified: direction-typed extension points required by the issue |

## Scope
- **In scope:**
  - `client-api/src/main/java/net/onestorm/bend/client/api/Client.java` (new)
  - `client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandle.java` (new)
  - `client-api/src/main/java/net/onestorm/bend/client/api/IncomingEndpoint.java` (new)
  - `client-api/src/main/java/net/onestorm/bend/client/api/OutgoingEndpoint.java` (new)
  - `client-api/src/main/java/net/onestorm/bend/client/api/EndpointCodec.java`, `EndpointHandler.java`, `package-info.java`: Javadoc links to the new types (BD-0002 follow-up)
  - `docs/decisions/0019-supported-ids-full-snapshot.md`: Context and Decision per D4
  - `docs/decisions/0011-protocol-exception.md` and root `CLAUDE.md` § API design decisions: `IllegalStateException` for state conflicts (D6)
  - Kendo BD-0003: acceptance criteria per D1 and D2 (done during planning)
- **Out of scope:**
  - `send`, its `CompletableFuture<Void>` and `EndpointUnavailableException` (BD-0004)
  - `close()`, connection state and registering after close (BD-0005)
  - Which thread runs handlers, thread safety of `register*`/`unregister`, and "no handler call after `unregister()` returns" (BD-0006)
  - Checking whether an endpoint is currently available; looking a handle up by id (issue Scope)
  - The `client-netty` implementation: registration table, supported-set computation, direction matching against the server's flags
  - A `String` overload of `register*` (D1)
  - Any change to `protocol` or `protocol/protocol-v1.md`
  - A test class in `client-api` (D5)

## Approach

1. **`EndpointHandle.java`**: `public interface EndpointHandle`. Class Javadoc: a handle to one registration of an endpoint on a `Client`, returned by `{@link Client#registerIncoming}` or `{@link Client#registerOutgoing}`; it belongs to the `Client` and stays valid across reconnects.
   - `NamespacedId getId()`: the id the endpoint was registered with. `@return`.
   - `void unregister()`: removes this registration. For an incoming endpoint, its handler receives no further messages (the exact timing is BD-0006's); while the id has no other registration on this client, the client stops declaring it supported to the server. A registration with the same id in the other direction is not affected. Calling it more than once does nothing after the first call, even when the same id has been registered again since (D3). After it, the same id may be registered again in the same direction.
2. **`IncomingEndpoint.java`**: `public interface IncomingEndpoint<T> extends EndpointHandle {}`. Javadoc: the handle of an endpoint the client receives on, whose messages are decoded by its codec and passed to its `{@link EndpointHandler}`; `@param <T>`.
3. **`OutgoingEndpoint.java`**: `public interface OutgoingEndpoint<T> extends EndpointHandle {}`. Javadoc: the handle of an endpoint the client sends on, whose messages are encoded by its `{@link EndpointCodec}`; `@param <T>`. No mention of `send` yet (BD-0004 adds it and its text).
4. **`Client.java`**: `public interface Client`. Class Javadoc: a client of a Bend server; created by an implementation's builder, which returns this interface, so it has no `connect()` and no settings (ADR 0018). Endpoints are registered here; registrations belong to the client, not to one connection, and survive reconnects: after a reconnect the client declares the same endpoints again. Registering an id in both directions makes a bidirectional endpoint.
   - `<T> OutgoingEndpoint<T> registerOutgoing(NamespacedId id, EndpointCodec<T> codec)`: `@param <T>`, `@param id`, `@param codec`, `@return the handle of the new registration`, `@throws NullPointerException if id or codec is null`, `@throws IllegalStateException if id is already registered as outgoing on this client`.
   - `<T> IncomingEndpoint<T> registerIncoming(NamespacedId id, EndpointCodec<T> codec, EndpointHandler<T> handler)`: same tags, with `handler`, and "already registered as incoming".
   - Both: the endpoint can be used once the server offers this id; registering does not wait for that.
   - Class Javadoc shows the intended usage with ids declared once as constants (D1), e.g. `static final NamespacedId CHAT = NamespacedId.of("bend:chat");` then `client.registerOutgoing(CHAT, codec)`, so an invalid id fails where the constant is defined.
5. **Javadoc links in existing files**: `EndpointCodec` class Javadoc names the codec given to `{@link Client#registerIncoming}`/`{@link Client#registerOutgoing}`; `EndpointHandler` class Javadoc says it is given to `{@link Client#registerIncoming}`; `package-info.java` names `{@link Client}` and the handles. Wording about "the send" in `EndpointCodec#encode` stays unlinked until BD-0004.
6. **ADR 0019** (D4): Context says the supported set is the available ids that have a local registration in either direction (incoming or outgoing). Decision lists three triggers: a new `Available Endpoints` list (including one that drops a registered id), a local registration whose id is available, and a local unregister that leaves an available id with no registration. Full snapshot unchanged. Does not decide gains: whether a registration whose direction the server's flags don't permit counts toward the set (the client-netty epic decides).
7. **ADR 0011 and `CLAUDE.md`** (D6): ADR 0011 Decision gains a bullet: a call that the object's current state forbids (registering an id already registered in that direction) raises `IllegalStateException`; `IllegalArgumentException` stays for a bad argument. Root `CLAUDE.md` § API design decisions line becomes: "Malformed or oversized wire input raises `ProtocolException`; caller misuse at setup time raises `IllegalArgumentException` for a bad argument and `IllegalStateException` for a call the current state forbids." Keep the ADR 0011 link and add the BD-0003 D6 link.
8. **Kendo BD-0003**: already updated during planning (signatures take `NamespacedId id`, parent `EndpointHandle`, invalid-id clause dropped, re-register rule added).
9. Run `./gradlew :bend-client-api:javadoc`, `./gradlew :bend-client-api:dependencies --configuration compileClasspath` and `./gradlew build`.

## Security & Cost Surface

### Untrusted wire input and allocation bounds
N/A: this issue adds no reader, writer or limit and reads nothing off the wire. The id a caller
registers is local setup input, validated by `NamespacedId.of` (`IllegalArgumentException`,
BD-0001 D6). The one wire-adjacent hand-off is for `client-netty`: it must match the server's
`Available Endpoints` names (already validated by `protocol`'s `Endpoint` constructor, ADR 0005)
against registered ids without calling `NamespacedId.of` on raw wire strings, so a hostile name
never surfaces as an `IllegalArgumentException` (BD-0001 PLAN Risks). Proof: N/A: no code.

### Connection lifecycle, timeouts and silent peers
The only lifecycle property fixed here is that registrations belong to the `Client`, not to a
connection: they survive a disconnect and are declared again after a reconnect, once the new
connection's `Available Endpoints` arrives. Registering never waits on the server, so a silent or
absent server cannot block `register*`; no state, timeout or disconnect path is added. What a caller
sees on disconnect, and registering after `close()`, are BD-0005's. Proof: planned in the
client-netty epic, `NettyClientTest#reconnect_registeredEndpoints_declaredSupportedAgain`.

### Threading and the event loop
Deferred to BD-0006, not N/A: `register*` and `unregister` are public methods a caller can call
from any thread, but until BD-0006 lands the interfaces promise nothing about thread safety or
about which thread runs the handler, consistent with BD-0002 D6. BD-0006's issue already commits
`register*` and `unregister` to "safe to call from any thread" and "after `unregister()` returns,
the handler isn't called again". This plan's `unregister` Javadoc deliberately says "receives no
further messages" without the timing, so BD-0006 adds it without contradicting anything. Proof:
deferred to BD-0006, planned in `client-netty` as
`NettyClientTest#unregister_fromOtherThreadDuringDispatch_noLaterHandlerCall`.

### Resource ownership
N/A: no buffer, no `ChannelFuture`. The registration table is per-`Client` state that only the
caller grows, one entry per `register*` call, and `unregister()` removes it; a peer cannot grow it.
Proof: N/A: no code.

### Spec conformance
Touches protocol-v1.md § Flow ("When the client's supported endpoints change, it sends the complete
current list, replacing the previous one"), § Endpoint message routing ("The server MUST block any
`Endpoint Message` from the client on an endpoint that is not in the client's current `Supported
Endpoints` list") and § Endpoint list updates ("Before sending the updated list ... the sender MUST
stop sending `Endpoint Message`s on any removed endpoints"). This issue's contract is what lets
`client-netty` meet them: outgoing registrations count toward the supported set (else the server
must block their messages), and `unregister` changes the set, so it resends (D4, ADR 0019). The
"stop sending before the updated list" rule binds `client-netty`'s order of operations on
unregister; BD-0004's issue makes `send` after the handle's own `unregister()` throw
`IllegalStateException` (caller bug), separate from `EndpointUnavailableException`, which fails the
future when the endpoint is merely not usable. On the client side, § Client-side disconnect
conditions ("The server sends an `Endpoint Message` on an endpoint that is not currently available,
or in a direction not permitted by that endpoint's flags") stays `client-netty`'s to enforce; this
issue does not change what "available" means. No spec edit, no version change, no check added. Proof: planned in the
client-netty epic, `NettyClientTest#register_outgoingOnlyAvailableId_declaredSupported`.

### Public API surface, module boundaries and enforcement
New public types in `net.onestorm.bend.client.api`: `Client` (`registerOutgoing`,
`registerIncoming`), `EndpointHandle` (`getId`, `unregister`), `IncomingEndpoint<T>`,
`OutgoingEndpoint<T>`, all with Javadoc. No Netty type in any signature, enforced at L1:
`client-api/build.gradle.kts` has no main dependency, so an `io.netty` import cannot compile; no
module gains a dependency. An invalid id is impossible at L1, since the parameter type is
`NamespacedId` (D1). The direction of a handle is fixed at L1 by its type, so BD-0004 can put
`send` on `OutgoingEndpoint` only. Caller misuse: duplicate registration in one direction is
`IllegalStateException` and `null` is `NullPointerException`, both L3 runtime checks in the
implementation, specified here with `@throws`; a second `unregister()` is a no-op by contract. No
send path is added, so ADR 0008's split is untouched. Proof:
`./gradlew :bend-client-api:dependencies --configuration compileClasspath` prints "No
dependencies"; `./gradlew :bend-client-api:javadoc` prints no "no comment" warning; the L3 checks
are planned as `NettyClientTest#registerOutgoing_sameIdTwice_throwsIllegalState` in the
client-netty epic.

### Runtime endpoint add and remove
This issue defines the API for adding and removing endpoints on a live client; the cycle itself is
`client-netty`'s, and the contract here commits it to the following. Add: `register*` returns at
once; if the id is in the most recently acknowledged `Available Endpoints`, the client resends the
full supported set including it (ADR 0019 trigger 2), and the endpoint is usable only after
`Supported Endpoints Acknowledged` for that revision (§ Endpoint list updates: "New endpoints ...
MUST NOT be used until the full acknowledge cycle ... is complete"); if not available, nothing is
sent until a list that contains it arrives (trigger 1). Remove: `unregister()` on the last
registration for an available id stops outgoing sends on it first, then resends the set without it
(trigger 3); incoming messages on it that arrive before the server acknowledges that list are still
valid on the wire, and the client drops them locally because no handler remains (the spec's
in-flight window is the server's to respect; the client never disconnects for a message on an id it
still declared; a message on an id the server never made available, or in a direction its flags
forbid, is § Client-side disconnect conditions' MUST-disconnect case, enforced in `client-netty`). A
handler call already running when `unregister()` is called finishes (timing is BD-0006).
Unregistering one half of a bidirectional pair leaves the id supported, so no resend. Server-side
removal: a new `Available Endpoints` without a registered id recomputes the set (available ∩
registered) and resends it without that id (trigger 1); the registration itself stays, so the id
comes back if a later list offers it again. Overlapping updates: re-registering after unregister
(D3) can happen before the removal snapshot was acknowledged. Each `Supported Endpoints Acknowledged`
enables only the ids of the revision it echoes (§ Revisions), so a stale acknowledgement of the
removal snapshot never enables the re-registered id; only the acknowledgement of the newer revision
does. Proof: planned in the client-netty epic,
`NettyClientTest#unregister_lastRegistrationForAvailableId_resendsSupportedWithoutIt`,
`NettyClientTest#unregister_oneHalfOfBidirectional_sendsNothing` and
`NettyClientTest#reregister_staleAcknowledgementOfRemoval_doesNotEnableId`.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | `Client` declares exactly `<T> OutgoingEndpoint<T> registerOutgoing(NamespacedId, EndpointCodec<T>)` and `<T> IncomingEndpoint<T> registerIncoming(NamespacedId, EndpointCodec<T>, EndpointHandler<T>)`, and nothing else | Read `Client.java`; `./gradlew :bend-client-api:compileJava` |
| 2 | `EndpointHandle` declares exactly `NamespacedId getId()` and `void unregister()`; `IncomingEndpoint<T>` and `OutgoingEndpoint<T>` extend it and declare nothing | Read the three sources |
| 3 | `Client` Javadoc states: built by an implementation's builder, registrations belong to the client and survive reconnects, registering in both directions makes a bidirectional endpoint, registering does not wait for the server; each register method has `@throws NullPointerException` and `@throws IllegalStateException` for a duplicate in that direction | Read `Client.java` |
| 4 | `EndpointHandle#unregister` Javadoc states: later calls do nothing and never affect a newer registration of the same id, the other direction is unaffected, the id may be registered again in the same direction, and the client stops declaring the id once no registration for it remains | Read `EndpointHandle.java` |
| 5 | Every public type and member, including type parameters, has Javadoc, and every `{@link}` resolves | `./gradlew :bend-client-api:javadoc` prints no `warning: no comment`, no `no @param for <T>` and no `reference not found` |
| 6 | `EndpointCodec` and `EndpointHandler` class Javadoc link to `Client#registerIncoming` (and the codec also to `Client#registerOutgoing`); `package-info.java` links `Client` and the handle types | Read the three files |
| 7 | `client-api` still has no main dependency | `./gradlew :bend-client-api:dependencies --configuration compileClasspath` prints "No dependencies" |
| 8 | ADR 0019's Context counts registrations in both directions, its Decision lists the three triggers, and its Does not decide names the direction-flag mismatch | Read `docs/decisions/0019-supported-ids-full-snapshot.md` |
| 9 | Kendo BD-0003's acceptance criteria match D1 to D4 | Read the issue via `mcp__kendo__prepare-issue-context-tool` |
| 10 | ADR 0011's Decision and root `CLAUDE.md` § API design decisions both name `IllegalStateException` for a call the current state forbids, with `IllegalArgumentException` kept for a bad argument | Read both files |
| 11 | The build passes | `./gradlew build` |

## Reuse and patterns

- `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`: call: the id type of every signature; mirror: Javadoc voice, `@throws NullPointerException if ... is {@code null}` wording
- `client-api/src/main/java/net/onestorm/bend/client/api/EndpointCodec.java` and `EndpointHandler.java`: call: parameter types; mirror: `@param <T>` and contract-in-Javadoc style; edit in place for links
- `client-api/src/main/java/net/onestorm/bend/client/api/package-info.java`: mirror: edit in place, same sentence style
- The pre-migration `Client` (see `.claude/local-references.md`): mirror the method names only; drop `send(Object)`, the `void` returns, `String name` and the name-only key that made bidirectional endpoints impossible
- `docs/decisions/0019-supported-ids-full-snapshot.md`: edit in place, keeping the ADR shape from `docs/decisions/README.md`

## Testing Strategy

No test class in this PR (D5): all four new types are interfaces without default methods, so
there is no code to execute. The behaviour the Javadoc promises is tested where it is implemented,
in the client-netty epic: duplicate registration per direction, a bidirectional pair, idempotent
`unregister()`, re-registration after unregister with the old handle staying dead, the supported
set on each ADR 0019 trigger, and registrations surviving a reconnect (the planned names in the
Security & Cost Surface Proof lines).

Verification for this PR: `./gradlew :bend-client-api:javadoc` (AC 5; warns on a missing comment
or `@param <T>`, confirmed in BD-0002's IMPLEMENTATION.md; the task itself does not fail, so read its
output), `./gradlew :bend-client-api:dependencies --configuration compileClasspath` (AC 7), then
`./gradlew build`.

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: no wire or rule change; the spec already requires full supported lists and blocks undeclared ids |
| New or changed public type or method in `client-api` | Javadoc on that type | Update: `Client`, `EndpointHandle`, `IncomingEndpoint`, `OutgoingEndpoint`; links in `EndpointCodec`, `EndpointHandler`, `package-info.java` |
| A design decision, layer rule, convention or module plan | `CLAUDE.md` / ADR | Update: ADR 0019 Context and Decision (D4); ADR 0011 Decision and the root `CLAUDE.md` § API design decisions line (D6). D1 to D3 are this issue's contract, carried by the Javadoc, and need no `CLAUDE.md` line |

## Edge Cases
- Same id registered twice as outgoing (or twice as incoming): the second call throws `IllegalStateException`; the first handle is unaffected.
- Same id registered once incoming and once outgoing: allowed, a bidirectional endpoint with two handles.
- Unregistering one half of a bidirectional pair: the other half stays registered and the id stays supported.
- `unregister()` called twice, or called on an old handle after the id was registered again: does nothing (D3).
- `null` id, codec or handler: `NullPointerException`.
- Registering an id the server doesn't offer: succeeds; the endpoint becomes usable if a later `Available Endpoints` offers it.
- Disconnect and reconnect: registrations stay; the client declares them again for the new connection.

## Risks
- **Contract ahead of the implementation.** Every rule here is L3 or L4 until `client-netty` exists; the planned test names in the Surface Proof lines are the hand-off and must be carried into the client-netty epic's issues.
- **Unregister ordering on the wire.** § Endpoint list updates requires `client-netty` to stop sending on a removed id before the new `Supported Endpoints` goes out; BD-0004's "send after own unregister throws" covers the caller's thread, but a send already queued on the event loop must be checked there. Verify in the client-netty epic.
- **Direction mismatch.** An outgoing registration for an id the server offers only as clientbound: whether it counts toward the supported set is not decided here (ADR 0019 Does not decide); BD-0004's `EndpointUnavailableException` is the caller-facing answer.
- **BD-0001 D2's stated reason changes.** `of(String)` was justified by "BD-0003 registers by `String`"; D1 changes that, but `of(String)` stays the factory callers use, so no change to `NamespacedId`.
