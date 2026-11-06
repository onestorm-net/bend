# BD-0005-client-lifecycle: Client lifecycle

**Date:** 2026-11-06
**Status:** Approved
**Issue:** [BD-0005](https://onestorm.kendo.dev/projects/1/issues/BD-0005)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0005-client-lifecycle/DECISIONS.md)

## Goal
A client author can see whether their client is connected, wait until it is ready, hear about
every connect and disconnect with the reason (including the server's `Disconnect` type), learn when
each of their endpoints can be used, and shut the client down for good.

Revised after the PR #9 review (2026-11-06): endpoint availability added (D8), `firstConnection()`
renamed `whenReady()` (D9), `ConnectionListener` generalised to `ClientListener` with
`addListener`/`removeListener` (D5).

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| States | Connecting, connected, closed; an implementation that stops trying closes the client (D1) | Issue scope: reconnect policy is client-netty's |
| `close()` | `Client extends AutoCloseable`, `void close()`, idempotent, final, does not wait for I/O; afterwards `register*` and `addListener` throw `IllegalStateException` (D2) | ADR 0011 Decision (call the current state forbids), BD-0003 D6; `Client#registerOutgoing` `@throws` wording |
| `send` after `close()` | Unchanged: `IllegalStateException`; a racing send throws or is dropped without a report (D3) | BD-0004 D2, D3; `OutgoingEndpoint#send` Javadoc already lists "the client is closed" |
| Connection state | `boolean isConnected()`: true once the client has sent `Available Endpoints Acknowledged` on the current connection (protocol-v1.md § Flow step 3), false otherwise and after close; reflects the latest state from any thread | protocol-v1.md § Flow step 3 ("Client MUST respond with `Available Endpoints Acknowledged`") |
| Waiting for the first connection | `CompletionStage<Void> whenReady()` (D4) | No blocking call the I/O thread could make; ADR 0012 (no Netty type) holds: `java.util.concurrent` only |
| Client events | `ClientListener` (`onConnect`, `onDisconnect(DisconnectInfo)`, both `default`; later client-wide events are new defaults), `Client#addListener` / `removeListener` (D5) | `EndpointHandler#handle` Javadoc: a throwing callback is reported, connection stays open |
| Endpoint availability | `EndpointHandle#isAvailable()`; `EndpointListener<H extends EndpointHandle>` (`onAvailable(H)`, `onUnavailable(H)`, both `default`) given with the registration: `registerOutgoing(id, codec, listener)` overload, `EndpointHandler<T> extends EndpointListener<IncomingEndpoint<T>>` (D8) | BD-0003: everything about an endpoint hangs off its registration and handle; `EndpointHandler` stays a `@FunctionalInterface` |
| Disconnect info | Final class `DisconnectInfo.of(origin, type, reason)` with getters, `equals`, `hashCode`, `toString` (D6) | `NamespacedId`: final class, static `of`, `Objects.requireNonNull`, getters, value equality |
| Disconnect type | `enum DisconnectType` with `getId()` and `fromId(int)`; unknown id from the wire read as `GENERIC` by client-netty (D7) | protocol-v1.md § Disconnect type table; ADR 0007; `DisconnectPacket#decode` reads `Type` unchecked |
| Disconnect origin | `enum DisconnectOrigin { SERVER, CLIENT, CONNECTION, CLOSED }` (D6) | protocol-v1.md § Client-side disconnect conditions (the `CLIENT` cases) |

**Layer compliance:** every new type is in `client-api` (`net.onestorm.bend.client.api`) and uses
only `java.lang`, `java.util` (`Objects`) and `java.util.concurrent` (`CompletionStage`).
`client-api/build.gradle.kts` declares only `testImplementation(libs.assertj.core)` and gains
nothing; no Lombok (client-api/CLAUDE.md).

**Module shape:**

| Class | Inputs/outputs | Hidden behaviour | Test seam | Shallow-test reckoning | Verdict |
|---|---|---|---|---|---|
| `Client` (new members) | `close()`, `isConnected()`, `whenReady()`, `add/removeListener` | the whole connection state machine, reconnects, handshake | client-netty: connect, drop, reconnect, close, observe events | A second lifecycle need (say, a "reconnecting now" signal) is a new `DisconnectOrigin` value or listener method, not a `Client` method per flow step | Deep |
| `ClientListener` | two callbacks | none (extension point) | client-netty event order | A new event kind adds a `default` method; extension point, exempt per the lens | Exempt (extension point) |
| `EndpointListener` | two callbacks, each given the handle | none (extension point) | `EndpointListenerTest` (defaults, lambda handler); event order in client-netty | A new endpoint event is a new `default` method; one listener type serves both directions through the type parameter | Exempt (extension point) |
| `EndpointHandle#isAvailable` | one query | the server's offer, flags, the acknowledge cycle and the connection | client-netty | A new availability condition changes the implementation, not the signature | Deep |
| `DisconnectInfo` | `of(origin, type, reason)`, three getters | null checks, value equality | `DisconnectInfoTest` | A new disconnect cause is a new `DisconnectOrigin` value (parameter), not a new factory | Exempt (value class) |
| `DisconnectType` | `getId()`, `fromId(int)` | the spec id table | `DisconnectTypeTest` | A new spec type is a new constant | Exempt (value enum) |
| `DisconnectOrigin` | four constants | none | via `DisconnectInfoTest` | A new cause is a new constant | Exempt (value enum) |

## Scope
- **In scope:** in `client-api/src/main/java/net/onestorm/bend/client/api/`: `Client.java` (new
  members, `registerOutgoing` overload, Javadoc), new `ClientListener.java`, `EndpointListener.java`,
  `DisconnectInfo.java`, `DisconnectOrigin.java`, `DisconnectType.java`; `EndpointHandle.java`
  (`isAvailable`, `unregister` after close); `EndpointHandler.java` (extends `EndpointListener`);
  `OutgoingEndpoint.java` (one sentence linking `isAvailable`); `package-info.java`; tests
  `DisconnectTypeTest`, `DisconnectInfoTest`, `ClientListenerTest`, `EndpointListenerTest`;
  `client-api/CLAUDE.md` rule lines; the Kendo BD-0005 acceptance criteria (D3).
- **Out of scope:** reconnect policy and every other setting (builder, client-netty epic); the
  client-netty implementation and its behaviour tests; which thread runs listeners and handlers
  and whether `close()` is safe from any thread (BD-0006); a `getState()` enum beyond
  `isConnected()`; a `Throwable` cause on `DisconnectInfo` (D6); a client-to-server `Disconnect`
  packet or any other change to `protocol` or `protocol-v1.md`; package layout (BD-0013); null
  annotations (BD-0014); per-endpoint connection events and a reason on `onUnavailable` (D8); a
  per-endpoint "wait until available" stage (D9); a subscription handle or sealed event types for
  listeners (D5).

## Approach

All in `client-api`, package `net.onestorm.bend.client.api`:

1. `DisconnectType.java`: enum `GENERIC(0)`, `PROTOCOL_ERROR(1)`, `UNSUPPORTED_VERSION(2)`,
   `TIMEOUT(3)`; `int getId()`; `static DisconnectType fromId(int id)` throwing
   `IllegalArgumentException` for any other id (D7). Javadoc on the type, on every constant (its
   spec id and meaning), on `getId()` and on `fromId()`, linking protocol-v1.md § Disconnect, so
   `javadoc` reports no missing comment. Test: `DisconnectTypeTest`.
2. `DisconnectOrigin.java`: enum `SERVER`, `CLIENT`, `CONNECTION`, `CLOSED`, each constant's Javadoc
   saying which types it carries (D6), and that for `SERVER` an unknown wire type reads as
   `GENERIC` (D7).
3. `DisconnectInfo.java`: final class mirroring `NamespacedId`: private constructor,
   `static of(DisconnectOrigin origin, DisconnectType type, String reason)` with
   `Objects.requireNonNull` per argument (empty reason allowed: the spec allows it),
   `getOrigin()`, `getType()`, `getReason()`, `equals`, `hashCode`, `toString`. Immutable, safe to
   share. Test: `DisconnectInfoTest`.
4. `ClientListener.java`: `default void onConnect()`, `default void onDisconnect(DisconnectInfo
   info)`. Javadoc: every attempt ends with exactly one `onDisconnect`, `onConnect` only when the
   attempt connected; events in order; a throwing listener is reported by the implementation and
   other listeners still get the event; no replay for a late listener, so add the listener before
   reading `isConnected()` (D5).
5. `Client.java`: `extends AutoCloseable`; add `boolean isConnected()`,
   `CompletionStage<Void> whenReady()` (Javadoc: completes once and never resets, fails with
   `IllegalStateException` naming the last disconnect when closed before connecting, can't be
   completed by the caller, and its callbacks may run on the implementation's I/O thread, so they
   must not block), `void addListener(ClientListener)`,
   `void removeListener(ClientListener)`, `@Override void close()`. Add
   `@throws IllegalStateException if the client is closed` to `registerOutgoing`,
   `registerIncoming` and `addListener`. Class Javadoc gains a lifecycle paragraph (D1,
   D2) and an example adding a listener and waiting on `whenReady()`.
   - `addListener(null)` and `removeListener(null)` throw
     `NullPointerException`; adding an added listener again does nothing; removing a listener
     that isn't added does nothing, also after close.
   - `isConnected()` Javadoc: the definition from Key Design Decisions; a snapshot that may change
     right after it returns.
   - `close()` Javadoc: D2; a message already decoded but not yet given to its handler when
     `close()` returns is dropped, a handler call already running finishes; listeners get the
     final `onDisconnect` (`CLOSED`) before the implementation releases them.
6. `EndpointHandle.java`: `unregister` Javadoc: after the client is closed it does nothing.
7. `package-info.java`: one sentence on the lifecycle types.
8. `client-api/CLAUDE.md`: three rule lines (Documentation Sync).
9. Kendo BD-0005: replace the close criterion per D3 and align the others with D1 to D9.
10. (PR #9 review) `EndpointListener.java`: `default onAvailable(H)` / `onUnavailable(H)`; Javadoc:
    given with the registration, starts unavailable, alternating events starting with
    `onAvailable`, in order, none after `unregister()` returns, a disconnect or close gives
    `onUnavailable`, `send` may still throw inside `onAvailable`, a throwing listener is reported
    (D8). `EndpointHandler` extends `EndpointListener<IncomingEndpoint<T>>`.
    `Client#registerOutgoing(id, codec, listener)` overload (`NullPointerException` for a `null`
    listener). `EndpointHandle#isAvailable()` with the per-direction definition; one sentence in
    `OutgoingEndpoint#send` that its state refusals are exactly `!isAvailable()`. Rename
    `firstConnection` to `whenReady` and say ready is not endpoint availability (D9);
    `ClientListener#onConnect` says endpoints are not usable yet. Test: `EndpointListenerTest`.

## Sweep Inventory

Rule added: once `close()` has returned, every `Client` and handle method that would start new work
throws `IllegalStateException`. Sites from `grep -n "^    [a-zA-Z<].*(" client-api/src/main/java/net/onestorm/bend/client/api/{Client,EndpointHandle,OutgoingEndpoint,IncomingEndpoint}.java`
plus the new `Client` members:

| Rule added | Sibling site | Status |
|---|---|---|
| Closed throws ISE | `Client#registerOutgoing` (both overloads) | Applied (`@throws`) |
| Closed throws ISE | `Client#registerIncoming` | Applied (`@throws`) |
| Closed throws ISE | `Client#addListener` | Applied (`@throws`) |
| Closed throws ISE | `OutgoingEndpoint#send` | Skipped: already lists "the client is closed" (BD-0004) |
| Closed throws ISE | `EndpointHandle#unregister` | Skipped: releases rather than starts work; does nothing after close (D2) |
| Closed throws ISE | `EndpointHandle#getId` | Skipped: a read of an immutable value |
| Closed throws ISE | `EndpointHandle#isAvailable` | Skipped: a read; returns `false` after close |
| Closed throws ISE | `Client#removeListener`, `isConnected`, `whenReady`, `close` | Skipped: reads or teardown, meaningful after close |

## Security & Cost Surface

### Untrusted wire input and allocation bounds
`client-api` reads nothing off the wire. The one wire value this contract interprets is the
`Disconnect` `Type` VarInt (server to client, decoded by client-netty with the existing
`DisconnectPacket#decode`, which does not range-check it) and its `Reason` (`String (256)`, already
bounded by `ProtocolUtil#readString` with `MAXIMUM_REASON_LENGTH`). The API must not let an out-of-range
type escape as an exception into the pipeline: `DisconnectType#fromId` throws
`IllegalArgumentException` for unknown ids, so client-netty checks the id and maps an unknown one to
`GENERIC` (D7) instead of calling `fromId` blind. Nothing is sized by these values. Proof:
`DisconnectTypeTest#fromId_unknownId_throws` (the contract); the client-netty mapping test belongs
to the client-netty epic.

### Connection lifecycle, timeouts and silent peers
The contract names three states (D1): connecting, connected (after § Flow step 3), closed. Happy
path: attempt → `onConnect` → `whenReady()` completes → `isConnected()` true. Failure walk:
TCP refused or reset, or a silent server hitting the § Client-side disconnect conditions 30-second
handshake or keep-alive timeout, each ends the attempt with one `onDisconnect`
(`CONNECTION`/`GENERIC`, or `CLIENT`/`TIMEOUT`); a server `Disconnect` in reply to `Handshake`
(unsupported version) ends it with `SERVER`/`UNSUPPORTED_VERSION` even though `onConnect` never
fired; invalid built-in packets, out-of-order packets, or a server `Endpoint Message` on an
unavailable endpoint or in a direction its flags forbid give `CLIENT`/`PROTOCOL_ERROR`; a server
that half-closes or closes without a `Disconnect` gives `CONNECTION`/`GENERIC`; local
`close()` gives `CLOSED`/`GENERIC`. The timeouts themselves (30 s, ADR 0006) and their
cancellation are client-netty's; this contract only fixes what the caller observes in each case, so
no failure is silent. `whenReady()` never hangs: it completes or the client closes (D1),
which fails it. Proof: N/A in `client-api`: no implementation to drive; `onDisconnect` exactly
once per attempt, with the origin and type above, is a client-netty epic test.

### Threading and the event loop
No code here runs on a thread; the contract avoids anything that forces a thread choice BD-0006
owns: `close()` does not wait for I/O (D2), so calling it from a handler or listener on the I/O
thread cannot deadlock, and there is no blocking `await` (D4); a caller that wants to block does it
on its own thread through the stage. The one ordering promise is that one client's events reach a
listener one at a time and in order (D5); client-netty must keep that across reconnects
(verify at implementation), and BD-0006 must not undo it when it picks the thread.
`isConnected()` may be called from any thread, so its Javadoc requires the latest state to be
visible there (the implementation needs a `volatile` or equivalent; client-netty's choice).
`whenReady()`'s Javadoc warns that its callbacks may run on the I/O thread. Which thread runs listeners and stage callbacks, and the listener
collection's thread safety, are BD-0006 and client-netty's. `DisconnectInfo` and both enums are
immutable. Endpoint listeners (D8) follow the same rules per endpoint: one at a time, in order,
alternating; `isAvailable()` must show the latest state on any thread. Proof: N/A in `client-api`:
no executable concurrency; client-netty epic tests the order.

### Resource ownership
No `ByteBuf`, channel or future crosses the API. New per-client state the caller controls: the
listener set, bounded by the caller's own adds and deduplicated by identity (D5); an endpoint
listener is one per registration and goes with it (D8). `close()` is the
point after which the implementation frees the connection, its scheduled timeouts and, after
delivering the final `onDisconnect` (`CLOSED`), the listener set; the contract promises only that no new work starts (D2), and client-netty owns the actual
release and its timing. `whenReady()` returns a stage the caller cannot complete, so one
caller can't corrupt it for others (D4). Proof: N/A in `client-api`: no allocation; release on
close is a client-netty epic test.

### Spec conformance
The contract maps protocol-v1.md onto the caller's view without changing the wire: "connected" is
the client has sent `Available Endpoints Acknowledged`, § Flow step 3 ("Client MUST respond
with `Available Endpoints Acknowledged`"), the step after which endpoint messages may flow; the
`DisconnectType` constants are § Disconnect's table (0 Generic, 1 Protocol error, 2 Unsupported
version, 3 Timeout, ADR 0007); `CLIENT` covers § Client-side disconnect conditions ("The client MUST
disconnect (give up on the connection) when ..."). The spec has no client-to-server `Disconnect`, so
`close()` sends nothing (D2). The spec's type table has no rule for an unknown type; D7 reads it as
type 0 ("Generic; See Reason") on the client and deliberately does not edit the spec: a
from-spec client in another language picks its own reading, which is harmless because the
connection is ending either way (the developer declined a spec note at plan approval). No spec edit, no version bump. Proof: `DisconnectTypeTest#getId_eachType_matchesSpecTable`.

### Public API surface, module boundaries and enforcement
New public types: `ClientListener`, `EndpointListener`, `DisconnectInfo`, `DisconnectOrigin`,
`DisconnectType`; new `Client` members `close`, `isConnected`, `whenReady`, `addListener`,
`removeListener` and a `registerOutgoing` overload; `EndpointHandle#isAvailable`;
`EndpointHandler` now extends `EndpointListener` (source compatible: a lambda for `handle` still
compiles, pinned by `EndpointListenerTest`). No Netty type (L1: `client-api` has no Netty dependency, so it cannot
compile). No module dependency change. No send path is added; built-ins stay internal (ADR 0008).
Caller misuse: registering or adding a listener after close throws `IllegalStateException`; `null`
arguments throw `NullPointerException`; `fromId` with an unknown id throws
`IllegalArgumentException` (ADR 0011). All new types and members carry Javadoc (ADR 0021). The new
rule "after close, starting new work throws" is L4 here (Javadoc and review), L2 once client-netty's
tests exist; the same holds for "an implementation that stops trying closes the client" (D1) and
"an unknown wire type reads as `GENERIC`" (D7). The trade-off is that `client-api` has no
implementation to test against yet.
Proof: `./gradlew :bend-client-api:javadoc --rerun` shows no warning; `DisconnectInfoTest#of_nullOrigin_throws`.

### Runtime endpoint add and remove
D8 makes the endpoint update cycle visible to callers without changing it. Add: the server sends a
new `Available Endpoints`; the client acknowledges it, sends `Supported Endpoints` with the id, and
only when that is acknowledged does the endpoint become available and `onAvailable` fire
(§ Endpoint list updates: "New endpoints in an updated list MUST NOT be used until the full
acknowledge cycle for that list is complete"). A new local registration of an id that is offered
follows the same cycle; one whose id is already declared and acknowledged (the other direction is
registered) becomes available straight away, and its listener still gets `onAvailable`, since it
was given with the registration. Remove by the server: the client stops sending on the id before
acknowledging the new list, so an outgoing endpoint becomes unavailable when the list arrives and
`send` throws from that point. An incoming endpoint becomes unavailable only after the client has
sent its acknowledgement: messages already in flight stay valid until then and still reach the
handler (§ Endpoint list updates: "in-flight messages on a removed endpoint remain valid until the
acknowledgement is sent"), so client-netty's "not currently available" disconnect check must use
the same boundary. An event racing such a change ends in `send` throwing, not a silent drop of a message the
caller thinks was sent. Overlapping updates resolve by revision (client-netty's, BD-0003 / ADR
0019); availability follows the acknowledged state only. Remove by the client (`unregister()`): no
event, the listener is done. After `close()` new registrations throw and `unregister()` does
nothing (D2); an incoming message decoded but not yet dispatched when `close()` returns is dropped,
and a handler call already running finishes. Proof: N/A in `client-api`: no implementation;
client-netty #4 (track the acknowledged set) tests the event per transition.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | `DisconnectType` has `GENERIC`, `PROTOCOL_ERROR`, `UNSUPPORTED_VERSION`, `TIMEOUT` with ids 0 to 3 and `fromId` round-trips them and throws `IllegalArgumentException` for any other id | `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectTypeTest' --rerun` |
| 2 | `DisconnectInfo.of` rejects each `null` argument with `NullPointerException`, accepts an empty reason, and two infos with equal fields are equal with equal hash codes | `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectInfoTest' --rerun` |
| 3 | `Client` extends `AutoCloseable` and declares `close()` without a checked exception, `isConnected()`, `whenReady()`, `addListener`, `removeListener`, and `registerOutgoing(id, codec, listener)` | `./gradlew :bend-client-api:build` passes; read `Client.java` |
| 3a | `EndpointHandle#isAvailable()` is defined per direction, matching `send`'s state refusals; `EndpointListener` states D8's event rules; `EndpointHandler` extends `EndpointListener<IncomingEndpoint<T>>` and a lambda for `handle` still compiles; both listener types' defaults do nothing | `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.EndpointListenerTest' --tests 'net.onestorm.bend.client.api.ClientListenerTest' --rerun`; read the Javadoc against D8 |
| 4 | Javadoc of `close`, `registerOutgoing`, `registerIncoming`, `addListener`, `EndpointHandle#unregister` states D2's after-close behaviour (including in-flight messages); `ClientListener` states D5's event rules; `DisconnectOrigin` states D6/D7's type per origin | read the Javadoc against D2, D5 to D7 |
| 4a | `whenReady()` Javadoc states D4 (completes once, never resets, fails with `IllegalStateException` when closed first, not completable by the caller, callbacks must not block); `isConnected()` Javadoc states the § Flow step 3 definition and cross-thread visibility; `add/removeListener` state `NullPointerException` for `null`, and duplicate add and unknown remove as no-ops | read the Javadoc against D4, D5 and Approach 5 |
| 4b | `package-info.java` names the lifecycle and availability types, and `client-api/CLAUDE.md` has the three rule lines from Documentation Sync, each linking this DECISIONS.md | read both files |
| 5 | `send`'s signature and refused states are unchanged (only a sentence linking `isAvailable` is added) and `client-api` has no per-send future | `git diff origin/development -- client-api/src/main/java/net/onestorm/bend/client/api/OutgoingEndpoint.java` shows only that Javadoc sentence |
| 6 | Javadoc builds cleanly | `./gradlew :bend-client-api:javadoc --rerun` shows no warning |
| 7 | Kendo BD-0005's acceptance criteria say `send` throws after close (D3) and describe D1 to D9 | read it with `prepare-issue-context-tool` |

## Reuse and patterns

- `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`: mirror: final value
  class, private constructor, static `of`, `Objects.requireNonNull`, getters, Javadoc'd
  `equals`/`hashCode`/`toString`.
- `client-api/src/test/java/net/onestorm/bend/client/api/NamespacedIdTest.java`: mirror: test
  naming and AssertJ style.
- `client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandler.java`: mirror: how a
  throwing callback is described ("the implementation reports the failure").
- `client-api/src/main/java/net/onestorm/bend/client/api/Client.java`: mirror: `@throws` wording for
  `NullPointerException` and `IllegalStateException`.
- `protocol/src/main/java/net/onestorm/bend/protocol/packet/DisconnectPacket.java`: read: `type` is
  an unchecked `int`, `reason` is bounded at 256 (D7).

## Testing Strategy

One PR, `client-api` only. Behaviour of `Client` (events, state, close) is tested in the
client-netty epic, as the issue says; there is no implementation here to test.

`DisconnectTypeTest` (`./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectTypeTest' --rerun`):

| Test | Behaviour | Red case |
|---|---|---|
| `getId_eachType_matchesSpecTable` | `GENERIC` 0, `PROTOCOL_ERROR` 1, `UNSUPPORTED_VERSION` 2, `TIMEOUT` 3, written as literals with a `// spec:` comment like `NamespacedIdTest` | fails when a constant's id or order drifts from the spec table |
| `fromId_eachSpecId_returnsType` (parameterized 0 to 3) | `fromId(getId())` returns the same constant | fails when the lookup maps an id to the wrong constant |
| `fromId_unknownId_throws` (parameterized -1, 4, `Integer.MAX_VALUE`) | throws `IllegalArgumentException` naming the id | fails when an unknown id silently maps to a constant |

`DisconnectInfoTest` (`./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectInfoTest' --rerun`):

| Test | Behaviour | Red case |
|---|---|---|
| `of_validArguments_returnsThem` | getters return origin, type and reason | fails when a getter returns the wrong field |
| `of_emptyReason_accepted` | `""` is a valid reason | fails when an empty server reason is rejected |
| `of_nullOrigin_throws`, `of_nullType_throws`, `of_nullReason_throws` | `NullPointerException` | fails when a `null` slips into the value |
| `equals_sameFields_equalWithSameHashCode` | equal fields, equal objects and hashes; a different field, not equal | fails when equality ignores a field |
| `toString_containsFields` | origin, type and reason appear | fails when `toString` drops a field |

`ClientListenerTest` and `EndpointListenerTest` (added at implementation and at the PR #9 review;
the JaCoCo gap check showed the `default` bodies are reachable lines):

| Test | Behaviour | Red case |
|---|---|---|
| `ClientListenerTest#onConnect_notOverridden_doesNothing`, `#onDisconnect_notOverridden_doesNothing` | a listener overriding nothing ignores both events | fails when a default throws |
| `EndpointListenerTest#onAvailable_notOverridden_doesNothing`, `#onUnavailable_notOverridden_doesNothing` | same for the endpoint listener | fails when a default throws |
| `EndpointListenerTest#endpointHandler_lambda_handlesAndIgnoresAvailability` | a method-reference `EndpointHandler` handles messages and ignores availability | fails (does not compile) when `EndpointHandler` stops being functional, or when a default acts |

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: no wire or rule change; D7 is the client's reading of an unknown type, not a spec rule |
| New or changed public type or method in `client-api` | Javadoc on that type | Update: `Client`, `ClientListener`, `EndpointListener`, `EndpointHandler`, `EndpointHandle` (`isAvailable`, `unregister`), `OutgoingEndpoint#send`, `DisconnectInfo`, `DisconnectOrigin`, `DisconnectType`, `package-info` |
| A design decision, layer rule or convention | `CLAUDE.md` | Update `client-api/CLAUDE.md`: "`close()` is final and does not wait for I/O; afterwards registering and adding a listener throw `IllegalStateException`; an implementation that stops reconnecting closes the client (D1, D2)" , "Client-wide events go to a `ClientListener` added on the `Client`; every attempt ends with one `onDisconnect` carrying a `DisconnectInfo` (D5, D6)" and "An endpoint's availability goes to the `EndpointListener` given with its registration; `isAvailable()` is false exactly when `send` would refuse for a state reason (D8)". Root `CLAUDE.md`: N/A, no cross-module rule |

## Edge Cases
- **Server rejects the version at handshake:** `onConnect` never fires; `onDisconnect` with
  `SERVER`/`UNSUPPORTED_VERSION`; with no further attempt the client closes and
  `whenReady()` fails naming that disconnect (D1, D4).
- **`close()` before the first connection:** `whenReady()` completes exceptionally with
  `IllegalStateException`; listeners get `onDisconnect` `CLOSED` if an attempt was in progress.
- **`close()` called twice, or from inside a listener or handler:** the second call does nothing;
  inside a callback it returns without waiting (D2).
- **`send` racing `close()` on another thread:** throws, or is accepted and dropped without a
  report (D3, BD-0004 D3).
- **Server sends `Disconnect` type 9:** `SERVER`/`GENERIC`, server's reason kept (D7).
- **Listener added while connected:** gets no `onConnect`; reads `isConnected()` after adding (D5).
- **Listener throws:** reported by the implementation; other listeners still get the event; the
  connection is unaffected (D5).
- **Adding the same listener twice, removing an unknown one:** no effect, no exception (Approach 5).
- **Server removes an endpoint while the caller sends from `onAvailable`:** `send` throws
  `IllegalStateException`; `onUnavailable` follows (D8).
- **Registering the outgoing side of an id whose incoming side is already available:** available at
  once; the new listener still gets `onAvailable` (D8).
- **`unregister()` while available:** no `onUnavailable`; the listener is not called again (D8).
- **Disconnect while endpoints are available:** each gets `onUnavailable`, and after the reconnect's
  acknowledge cycle `onAvailable` again (D8).

## Risks
- `Client extends AutoCloseable` makes compilers warn about a `Client` not closed in
  try-with-resources, which is noisy for long-lived clients; accepted for the try-with-resources
  use in tests and short tools.
- D1 (giving up means closed) commits client-netty's reconnect design: a builder option like
  "retry forever" never closes, and "no reconnect" closes after the first attempt. If the
  developer wants a client that can be told to retry later, D1 is wrong.
- `CompletionStage` callbacks run on whichever thread completes them, which is the I/O thread
  unless BD-0006 says otherwise; a caller chaining blocking work there stalls the connection.
- The ordering promise to listeners (D5) is made before BD-0006 picks threads; BD-0006 must keep
  it, or revise D5 explicitly.
- Availability depends on the acknowledged `Supported Endpoints` state that client-netty #4 tracks;
  if that tracking and `send`'s checks drift apart, `isAvailable()` stops matching `send`. client-netty
  should derive both from one state.
- `minimalCompletionStage` behaviour on `toCompletableFuture()` (returns a new, independent
  future) must be checked on JDK 25 at implementation, or a defensive copy used.
