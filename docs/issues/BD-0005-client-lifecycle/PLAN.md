# BD-0005-client-lifecycle: Client lifecycle

**Date:** 2026-11-06
**Status:** Revision approved (2026-11-09)
**Issue:** [BD-0005](https://onestorm.kendo.dev/projects/1/issues/BD-0005)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0005-client-lifecycle/DECISIONS.md)

## Goal
A client author can see whether their client is connected, wait until it is ready, hear about
every connect and disconnect with the reason (including the server's `Disconnect` type), from the
very first connection attempt on, and shut the client down for good.

Revised after the PR #9 review (2026-11-06): `firstConnection()` renamed `whenReady()` (D9),
`ConnectionListener` generalised to `ClientListener` with `addListener`/`removeListener` (D5).

Revised after the PR #9 scope check (2026-11-09):

- Endpoint availability (D8) is dropped from this issue: BD-0005 did not ask for it. It moves to a
  separate design issue for endpoint registration and availability. Everything D8 added is
  removed (Approach 11).
- New: a `ClientBuilder` interface in `client-api`, so a `ClientListener` can be added before the
  client connects and hears the first attempt's events (D10, Approach 12).

Revised after the developer's review of PR #9 (2026-11-11): `DisconnectOrigin` is dropped for
v0.1 (D6 revised); `DisconnectInfo` carries only the type and the reason (Approach 13).

## Questions settled at the 2026-11-09 revision

The developer approved the proposed answer to each (D10).

1. **Builder method name.** Proposed: `ClientBuilder#addListener`, the same name and semantics as
   `Client#addListener` (adding the same listener twice does nothing). Alternative:
   `withListener`, the usual fluent-builder prefix, but nothing in Bend uses `with*` yet and two
   names for one act would differ only by where they are called.
2. **Does `Client` keep `addListener`/`removeListener`?** Proposed: keep both. D5 rejected
   "listeners only in the builder" twice, because code that only holds a `Client` (handed one,
   not building it) must still be able to listen and stop listening. The builder method is for the
   one case `Client#addListener` can't serve: the first attempt's events. Alternative: drop them
   and make the builder the only place to add a listener (D5 then changes: such code can't listen).
3. **A second `connect()` on the same builder.** Proposed: throws `IllegalStateException`, as does
   `addListener` after `connect()`; one builder makes one client, so two clients never share
   listener instances by accident. Alternative: every `connect()` makes a new, independent client
   with the listeners added so far.

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| States | Connecting, connected, closed; an implementation that stops trying closes the client (D1) | Issue scope: reconnect policy is client-netty's |
| `close()` | `Client extends AutoCloseable`, `void close()`, idempotent, final, does not wait for I/O; afterwards `register*` and `addListener` throw `IllegalStateException` (D2) | ADR 0011 Decision (call the current state forbids), BD-0003 D6; `Client#registerOutgoing` `@throws` wording |
| `send` after `close()` | Unchanged: `IllegalStateException`; a racing send throws or is dropped without a report (D3) | BD-0004 D2, D3; `OutgoingEndpoint#send` Javadoc already lists "the client is closed" |
| Connection state | `boolean isConnected()`: true once the client has sent `Available Endpoints Acknowledged` on the current connection (protocol-v1.md § Flow step 3), false otherwise and after close; reflects the latest state from any thread | protocol-v1.md § Flow step 3 ("Client MUST respond with `Available Endpoints Acknowledged`") |
| Waiting for the first connection | `CompletionStage<Void> whenReady()` (D4, D9) | No blocking call the I/O thread could make; ADR 0012 (no Netty type) holds: `java.util.concurrent` only |
| Client events | `ClientListener` (`onConnect`, `onDisconnect(DisconnectInfo)`, both `default`; later client-wide events are new defaults), `Client#addListener` / `removeListener` (D5) | `EndpointHandler#handle` Javadoc: a throwing callback is reported, connection stays open |
| Listener before connecting | `interface ClientBuilder` in `client-api`: `ClientBuilder addListener(ClientListener)` and `Client connect()`; an implementation's builder implements it and keeps all its own settings (D10) | ADR 0018 Decision (builder's `connect()` returns `Client`; `Client` has no `connect()`), its "Does not decide" leaves builder method names open |
| Endpoint availability | Dropped (D8): moves to a separate design issue | Scope check of PR #9: not in BD-0005 |
| Disconnect info | Final class `DisconnectInfo.of(type, reason)` with getters, `equals`, `hashCode`, `toString` (D6) | `NamespacedId`: final class, static `of`, `Objects.requireNonNull`, getters, value equality |
| Disconnect type | `enum DisconnectType` with `getId()` and `fromId(int)`; unknown id from the wire read as `GENERIC` by client-netty (D7) | protocol-v1.md § Disconnect type table; ADR 0007; `DisconnectPacket#decode` reads `Type` unchecked |
| Disconnect origin | Dropped (D6 revised, 2026-11-11): not important for v0.1 | Developer's review of PR #9 |

**Layer compliance:** every new type is in `client-api` (`net.onestorm.bend.client.api`) and uses
only `java.lang`, `java.util` (`Objects`) and `java.util.concurrent` (`CompletionStage`).
`client-api/build.gradle.kts` declares only `testImplementation(libs.assertj.core)` and gains
nothing; no Lombok (client-api/CLAUDE.md). `ClientBuilder` holds no setting and no
implementation: ADR 0018's "`client-api` holds no configuration" and "`Client` has no
`connect()`" both still hold; the builder's settings stay the implementation's.

**Module shape:**

| Class | Inputs/outputs | Hidden behaviour | Test seam | Shallow-test reckoning | Verdict |
|---|---|---|---|---|---|
| `Client` (new members) | `close()`, `isConnected()`, `whenReady()`, `add/removeListener` | the whole connection state machine, reconnects, handshake | client-netty: connect, drop, reconnect, close, observe events | A second lifecycle need (say, a "reconnecting now" signal) is a new `DisconnectType` value or listener method, not a `Client` method per flow step | Deep |
| `ClientListener` | two callbacks | none (extension point) | client-netty event order | A new event kind adds a `default` method; extension point, exempt per the lens | Exempt (extension point) |
| `ClientBuilder` | `addListener`, `connect()` | creating the client and starting its first attempt with the listeners in place | client-netty's builder: a listener added there gets the first attempt's events | Only what code holding just the interface needs; every setting stays on the implementation's builder, so no new setting lands here | Exempt (builder: collects and constructs) |
| `DisconnectInfo` | `of(type, reason)`, two getters | null checks, value equality | `DisconnectInfoTest` | A new disconnect cause is a new `DisconnectType` value (parameter), not a new factory | Exempt (value class) |
| `DisconnectType` | `getId()`, `fromId(int)` | the spec id table | `DisconnectTypeTest` | A new spec type is a new constant | Exempt (value enum) |

## Scope
- **In scope:** in `client-api/src/main/java/net/onestorm/bend/client/api/`: `Client.java` (new
  members, Javadoc), new `ClientListener.java`, `ClientBuilder.java`, `DisconnectInfo.java`,
  `DisconnectType.java`; `EndpointHandle.java` (`unregister` after
  close); `package-info.java`; tests `DisconnectTypeTest`, `DisconnectInfoTest`,
  `ClientListenerTest`; `client-api/CLAUDE.md` rule lines; the Kendo BD-0005 acceptance criteria
  (D3, D6 revised, D8 dropped, D10).
- **Out of scope:** reconnect policy and every other setting, and the concrete builder
  (client-netty epic); the client-netty implementation and its behaviour tests; which thread runs
  listeners and handlers and whether `close()` is safe from any thread (BD-0006); a `getState()`
  enum beyond `isConnected()`; a `Throwable` cause on `DisconnectInfo` (D6); a client-to-server
  `Disconnect` packet or any other change to `protocol` or `protocol-v1.md`; package layout
  (BD-0013); null annotations (BD-0014); endpoint availability, `isAvailable()` and endpoint
  listeners (D8, dropped: the separate endpoint registration and availability design issue);
  registering endpoints on the builder (D10); a subscription handle or sealed event types for
  listeners (D5).

## Approach

All in `client-api`, package `net.onestorm.bend.client.api`:

1. `DisconnectType.java`: enum `GENERIC(0)`, `PROTOCOL_ERROR(1)`, `UNSUPPORTED_VERSION(2)`,
   `TIMEOUT(3)`; `int getId()`; `static DisconnectType fromId(int id)` throwing
   `IllegalArgumentException` for any other id (D7). Javadoc on the type, on every constant (its
   spec id and meaning), on `getId()` and on `fromId()`, linking protocol-v1.md § Disconnect, so
   `javadoc` reports no missing comment. Test: `DisconnectTypeTest`.
2. (Dropped by step 13) `DisconnectOrigin.java`: enum `SERVER`, `CLIENT`, `CONNECTION`, `CLOSED`, each constant's Javadoc
   saying which types it carries (D6), and that for `SERVER` an unknown wire type reads as
   `GENERIC` (D7).
3. `DisconnectInfo.java`: final class mirroring `NamespacedId`: private constructor,
   `static of(DisconnectType type, String reason)` (step 13; first with a `DisconnectOrigin`) with
   `Objects.requireNonNull` per argument (empty reason allowed: the spec allows it),
   `getType()`, `getReason()`, `equals`, `hashCode`, `toString`. Immutable, safe to
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
     final `onDisconnect` (`GENERIC`) before the implementation releases them.
6. `EndpointHandle.java`: `unregister` Javadoc: after the client is closed it does nothing.
7. `package-info.java`: one sentence on the lifecycle types.
8. `client-api/CLAUDE.md`: rule lines (Documentation Sync).
9. Kendo BD-0005: replace the close criterion per D3 and align the others with the decisions in
   force.
10. (PR #9 review, 2026-11-06) Rename `firstConnection` to `whenReady` (D9) and
    `ConnectionListener` to `ClientListener` with `addListener`/`removeListener` (D5). The
    endpoint availability part of that review (D8) is undone by step 11.
11. (Scope check, 2026-11-09) Remove D8:
    - Delete `EndpointListener.java` and `EndpointListenerTest.java`.
    - `EndpointHandle.java`: delete `isAvailable()` and its Javadoc (keep step 6).
    - `EndpointHandler.java` and `OutgoingEndpoint.java`: back to `origin/development` exactly (no
      `extends EndpointListener`, no `isAvailable` sentence in `send`'s `@throws`).
    - `Client.java`: delete the `registerOutgoing(id, codec, listener)` overload; the two-argument
      `registerOutgoing` gets its `origin/development` Javadoc back plus step 5's "or the client is
      closed"; `registerIncoming`'s `handler` param loses the `EndpointListener` clause; the class
      Javadoc's endpoint paragraph ends with development's "an endpoint can be used once the
      server offers its id"; `whenReady()` keeps "ready means connected, not that a given
      endpoint can be used" without the `EndpointListener` pointer.
    - `ClientListener.java`: drop the `EndpointListener` sentences (class Javadoc and
      `onConnect`); `onConnect` keeps "endpoints are not usable yet".
    - `package-info.java`: drop the `isAvailable`/`EndpointListener` sentence.
    - `client-api/CLAUDE.md`: drop the endpoint listener rule line.
    - `docs/issues/BD-0004-send-returns-completable-future/DECISIONS.md`: back to
      `origin/development` (no "superseded in part by BD-0005 D8").
    - `IMPLEMENTATION.md`: a new entry logs the removal; the older entries stay as history.
    - The stale `registerOutgoing` links at `EndpointHandle.java:7` and `OutgoingEndpoint.java:7`
      name the two-argument overload, which is again the only one: they become correct without a
      change; verify after the removal.
12. (Scope check, 2026-11-09) `ClientBuilder.java` (D10): `ClientBuilder addListener(ClientListener
    listener)` and `Client connect()`, both abstract, no settings. Javadoc:
    - Type: an implementation's builder implements this interface and adds its own settings; code
      that is handed a `ClientBuilder` can add listeners and connect without knowing the
      implementation. An implementation may narrow `addListener`'s return type to its own builder
      so its settings still chain.
    - `addListener`: the listener is in place before the first connection attempt starts, so it
      gets that attempt's `onConnect` and `onDisconnect`; after `connect()` it is a listener of the
      client as if added with `Client#addListener`, removable with `Client#removeListener`. Adding
      the same listener twice does nothing. `NullPointerException` for `null`;
      `IllegalStateException` after `connect()` (question 3).
    - `connect()`: returns at once with a client that is connecting; it does not wait for the
      connection (use `whenReady()`). A failed attempt does not throw here: it arrives as
      `onDisconnect`, and the client follows D1. `IllegalStateException` when called a second time
      (question 3).
    - `Client` class Javadoc: "created by an implementation's builder, which implements
      `ClientBuilder` and returns this interface"; the lifecycle example adds the listener on the
      builder. `Client#addListener` Javadoc: to hear the first attempt, add the listener on the
      `ClientBuilder` instead. `ClientListener` class Javadoc: added with
      `ClientBuilder#addListener` before connecting or `Client#addListener` afterwards.
    - `package-info.java`: names `ClientBuilder` where it says a client is created by an
      implementation's builder.
13. (Developer's review of PR #9, 2026-11-11) Remove `DisconnectOrigin` (D6 revised):
    - Delete `DisconnectOrigin.java`.
    - `DisconnectInfo.java`: `of(DisconnectType type, String reason)`, no `getOrigin()`; `equals`,
      `hashCode`, `toString` over type and reason.
    - `DisconnectType.java`: the class Javadoc lists which type each kind of disconnect gets
      (server `Disconnect`, unknown wire type, client gives up, failed or lost connection,
      `close()`), the text `DisconnectOrigin`'s constants carried.
    - `Client#close` Javadoc: the final `onDisconnect` has type `GENERIC`; `package-info.java`:
      the info carries a type and a reason.
    - `DisconnectInfoTest` and `ClientListenerTest` without the origin.
    - This plan's Surface and Edge Cases prose names types only, no origin values.

## Sweep Inventory

Rule added: once `close()` has returned, every `Client` and handle method that would start new work
throws `IllegalStateException`. Sites from `grep -n "^    [a-zA-Z<].*(" client-api/src/main/java/net/onestorm/bend/client/api/{Client,EndpointHandle,OutgoingEndpoint,IncomingEndpoint}.java`
plus the new `Client` members:

| Rule added | Sibling site | Status |
|---|---|---|
| Closed throws ISE | `Client#registerOutgoing` | Applied (`@throws`) |
| Closed throws ISE | `Client#registerIncoming` | Applied (`@throws`) |
| Closed throws ISE | `Client#addListener` | Applied (`@throws`) |
| Closed throws ISE | `OutgoingEndpoint#send` | Skipped: already lists "the client is closed" (BD-0004) |
| Closed throws ISE | `EndpointHandle#unregister` | Skipped: releases rather than starts work; does nothing after close (D2) |
| Closed throws ISE | `EndpointHandle#getId` | Skipped: a read of an immutable value |
| Closed throws ISE | `Client#removeListener`, `isConnected`, `whenReady`, `close` | Skipped: reads or teardown, meaningful after close |
| Closed throws ISE | `ClientBuilder#addListener`, `connect` | Skipped: no client exists before `connect()`; after it, both throw for "already connected" instead (D10) |

## Security & Cost Surface

### Untrusted wire input and allocation bounds
`client-api` reads nothing off the wire. The one wire value this contract interprets is the
`Disconnect` `Type` VarInt (server to client, decoded by client-netty with the existing
`DisconnectPacket#decode`, which does not range-check it) and its `Reason` (`String (256)`, already
bounded by `ProtocolUtil#readString` with `MAXIMUM_REASON_LENGTH`). The API must not let an out-of-range
type escape as an exception into the pipeline: `DisconnectType#fromId` throws
`IllegalArgumentException` for unknown ids, so client-netty catches that exception and reports
`GENERIC` with the server's reason kept (D7), keeping the 0 to 3 range only in `DisconnectType`. Nothing is sized by these values. Proof:
`DisconnectTypeTest#fromId_unknownId_throws` (the contract); the client-netty mapping test belongs
to the client-netty epic.

### Connection lifecycle, timeouts and silent peers
The contract names three states (D1): connecting, connected (after § Flow step 3), closed. Happy
path: `connect()` → attempt → `onConnect` → `whenReady()` completes → `isConnected()` true. A
listener added on the `ClientBuilder` sees this from the first attempt (D10). Failure walk:
TCP refused or reset, or a silent server hitting the § Client-side disconnect conditions 30-second
handshake or keep-alive timeout, each ends the attempt with one `onDisconnect`
(`GENERIC`, or `TIMEOUT`); a server `Disconnect` in reply to `Handshake`
(unsupported version) ends it with `UNSUPPORTED_VERSION` even though `onConnect` never
fired; invalid built-in packets, out-of-order packets, or a server `Endpoint Message` on an
unavailable endpoint or in a direction its flags forbid give `PROTOCOL_ERROR`; a server
that half-closes or closes without a `Disconnect` gives `GENERIC`; local
`close()` gives `GENERIC`. The timeouts themselves (30 s, ADR 0006) and their
cancellation are client-netty's; this contract only fixes what the caller observes in each case, so
no failure is silent. `whenReady()` never hangs: it completes or the client closes (D1),
which fails it. `connect()` never throws for a failed attempt (D10). Proof: N/A in `client-api`:
no implementation to drive; `onDisconnect` exactly once per attempt, with the type
above, and the builder listener getting the first attempt, are client-netty epic tests.

### Threading and the event loop
No code here runs on a thread; the contract avoids anything that forces a thread choice BD-0006
owns: `close()` does not wait for I/O (D2), so calling it from a handler or listener on the I/O
thread cannot deadlock, `connect()` does not wait for the connection (D10), and there is no
blocking `await` (D4); a caller that wants to block does it on its own thread through the stage.
The one ordering promise is that one client's events reach a listener one at a time and in order
(D5); client-netty must keep that across reconnects (verify at implementation), and BD-0006 must
not undo it when it picks the thread. `isConnected()` may be called from any thread, so its Javadoc
requires the latest state to be visible there (the implementation needs a `volatile` or
equivalent; client-netty's choice). `whenReady()`'s Javadoc warns that its callbacks may run on
the I/O thread. Which thread runs listeners and stage callbacks, and the listener collection's
thread safety, are BD-0006 and client-netty's. A `ClientBuilder` is used by one thread before
`connect()`; its Javadoc does not promise thread safety. `DisconnectInfo` and both enums are
immutable. Proof: N/A in `client-api`: no executable concurrency; client-netty epic tests the
order.

### Resource ownership
No `ByteBuf`, channel or future crosses the API. New per-client state the caller controls: the
listener set, bounded by the caller's own adds and deduplicated by identity (D5), whether added on
the builder or the client (D10). `close()` is the point after which the implementation frees the
connection, its scheduled timeouts and, after delivering the final `onDisconnect` (`GENERIC`), the
listener set; the contract promises only that no new work starts (D2), and client-netty owns the
actual release and its timing. One builder makes one client (D10), so no listener instance is
shared between clients by accident. `whenReady()` returns a stage the caller cannot complete, so
one caller can't corrupt it for others (D4). Proof: N/A in `client-api`: no allocation; release on
close is a client-netty epic test.

### Spec conformance
The contract maps protocol-v1.md onto the caller's view without changing the wire: "connected" is
the client has sent `Available Endpoints Acknowledged`, § Flow step 3 ("Client MUST respond
with `Available Endpoints Acknowledged`"), the step after which endpoint messages may flow; the
`DisconnectType` constants are § Disconnect's table (0 Generic, 1 Protocol error, 2 Unsupported
version, 3 Timeout, ADR 0007); `TIMEOUT` and `PROTOCOL_ERROR` also cover § Client-side disconnect conditions ("The client MUST
disconnect (give up on the connection) when ..."). The spec has no client-to-server `Disconnect`, so
`close()` sends nothing (D2). The spec's type table has no rule for an unknown type; D7 reads it as
type 0 ("Generic; See Reason") on the client and deliberately does not edit the spec: a
from-spec client in another language picks its own reading, which is harmless because the
connection is ending either way (the developer declined a spec note at plan approval). The
builder is not on the wire. No spec edit, no version bump. Proof:
`DisconnectTypeTest#getId_eachType_matchesSpecTable`.

### Public API surface, module boundaries and enforcement
New public types: `ClientListener`, `ClientBuilder`, `DisconnectInfo`, `DisconnectType`; new `Client` members `close`, `isConnected`, `whenReady`, `addListener`,
`removeListener`. Compared with the PR as it stands, `EndpointListener`,
`EndpointHandle#isAvailable`, the three-argument `registerOutgoing` and `EndpointHandler extends
EndpointListener` are removed again (D8 dropped); none of them is on `development`, so nothing
released changes. No Netty type (L1: `client-api` has no Netty dependency, so it cannot compile).
No module dependency change. No send path is added; built-ins stay internal (ADR 0008).
`ClientBuilder` fixes two method names for every implementation's builder, which ADR 0018 leaves
open ("Does not decide: the builder's settings or method names"); its Decision ("`Client` has no
`connect()` and no settings", "the builder's `connect()` returns the `Client` interface") holds
unchanged. Caller misuse: registering or adding a listener after close, and using a builder after
`connect()`, throw `IllegalStateException`; `null` arguments throw `NullPointerException`; `fromId`
with an unknown id throws `IllegalArgumentException` (ADR 0011). All new types and members carry
Javadoc (ADR 0021). The rules "after close, starting new work throws", "an implementation that
stops trying closes the client" (D1), "an unknown wire type reads as `GENERIC`" (D7) and "a
builder listener hears the first attempt" (D10) are L4 here (Javadoc and review), L2 once
client-netty's tests exist. The trade-off is that `client-api` has no implementation to test
against yet.
Proof: `./gradlew :bend-client-api:javadoc --rerun` shows no warning; `DisconnectInfoTest#of_nullType_throws`.

### Runtime endpoint add and remove
N/A since the 2026-11-09 revision: this issue no longer touches endpoints beyond D2's
`unregister` after close. Making the endpoint update cycle visible to callers (D8) moves to the
separate endpoint registration and availability design issue, which takes over the spec reading
the 2026-11-06 revision had here (§ Endpoint list updates: outgoing unusable when the list arrives,
incoming only after the acknowledgement). After `close()` new registrations throw and
`unregister()` does nothing (D2); an incoming message decoded but not yet dispatched when `close()`
returns is dropped, and a handler call already running finishes. Proof: N/A in `client-api`: no
implementation.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | `DisconnectType` has `GENERIC`, `PROTOCOL_ERROR`, `UNSUPPORTED_VERSION`, `TIMEOUT` with ids 0 to 3 and `fromId` round-trips them and throws `IllegalArgumentException` for any other id | `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectTypeTest' --rerun` |
| 2 | `DisconnectInfo.of` rejects each `null` argument with `NullPointerException`, accepts an empty reason, and two infos with equal fields are equal with equal hash codes | `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectInfoTest' --rerun` |
| 3 | `Client` extends `AutoCloseable` and declares `close()` without a checked exception, `isConnected()`, `whenReady()`, `addListener`, `removeListener`, and exactly one `registerOutgoing(id, codec)` | `./gradlew :bend-client-api:build` passes; read `Client.java` |
| 3a | `ClientBuilder` declares `addListener(ClientListener)` returning `ClientBuilder` and `Client connect()`, nothing else; its Javadoc states D10 (first attempt heard, returns at once, failures as `onDisconnect`, one client per builder) | read `ClientBuilder.java` against D10 |
| 3b | No trace of D8 in code or rules: `EndpointListener` and `EndpointListenerTest` are gone, `EndpointHandle` has no `isAvailable`, and `EndpointHandler.java`, `OutgoingEndpoint.java` and BD-0004's `DECISIONS.md` equal `origin/development` | `grep -rn "EndpointListener\|isAvailable" client-api CLAUDE.md` finds nothing; `git diff origin/development -- client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandler.java client-api/src/main/java/net/onestorm/bend/client/api/OutgoingEndpoint.java docs/issues/BD-0004-send-returns-completable-future/DECISIONS.md` is empty |
| 4 | Javadoc of `close`, `registerOutgoing`, `registerIncoming`, `addListener`, `EndpointHandle#unregister` states D2's after-close behaviour (including in-flight messages); `ClientListener` states D5's event rules and both places to add it; `DisconnectType` states D6/D7's type per kind of disconnect | read the Javadoc against D2, D5 to D7, D10 |
| 4a | `whenReady()` Javadoc states D4 (completes once, never resets, fails with `IllegalStateException` when closed first, not completable by the caller, callbacks must not block); `isConnected()` Javadoc states the § Flow step 3 definition and cross-thread visibility; `add/removeListener` state `NullPointerException` for `null`, and duplicate add and unknown remove as no-ops | read the Javadoc against D4, D5 and Approach 5 |
| 4b | `package-info.java` names the lifecycle types and `ClientBuilder`, and `client-api/CLAUDE.md` has the rule lines from Documentation Sync, each linking this DECISIONS.md | read both files |
| 5 | `send`'s signature, refused states and Javadoc are unchanged and `client-api` has no per-send future | `git diff origin/development -- client-api/src/main/java/net/onestorm/bend/client/api/OutgoingEndpoint.java` is empty |
| 6 | Javadoc builds cleanly | `./gradlew :bend-client-api:javadoc --rerun` shows no warning |
| 3c | No trace of `DisconnectOrigin` in code, rules or this plan's current prose (D6 revised) | `grep -rn "DisconnectOrigin\|getOrigin" client-api CLAUDE.md` finds nothing; PLAN.md names it only in the revision notes, Approach 2 (marked dropped) and Approach 13 |
| 7 | Kendo BD-0005's acceptance criteria say `send` throws after close (D3), describe D1 to D7, D9 and D10, and no longer ask for endpoint availability (D8) | read it with `prepare-issue-context-tool` |

## Reuse and patterns

- `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`: mirror: final value
  class, private constructor, static `of`, `Objects.requireNonNull`, getters, Javadoc'd
  `equals`/`hashCode`/`toString`.
- `client-api/src/test/java/net/onestorm/bend/client/api/NamespacedIdTest.java`: mirror: test
  naming and AssertJ style.
- `client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandler.java`: mirror: how a
  throwing callback is described ("the implementation reports the failure").
- `client-api/src/main/java/net/onestorm/bend/client/api/Client.java`: mirror: `@throws` wording for
  `NullPointerException` and `IllegalStateException`; `addListener` Javadoc for
  `ClientBuilder#addListener`.
- `docs/decisions/0018-client-built-by-implementation-builder.md`: read: the
  `NettyClient.builder()...connect()` shape `ClientBuilder` captures.
- `protocol/src/main/java/net/onestorm/bend/protocol/packet/DisconnectPacket.java`: read: `type` is
  an unchecked `int`, `reason` is bounded at 256 (D7).

## Testing Strategy

One PR, `client-api` only. Behaviour of `Client` and of a builder (events, state, close, first
attempt heard) is tested in the client-netty epic, as the issue says; there is no implementation
here to test. `ClientBuilder` has no `default` method, so it has no reachable line in
`client-api` and gets no test (ADR 0009: no contrived test).

`DisconnectTypeTest` (`./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectTypeTest' --rerun`):

| Test | Behaviour | Red case |
|---|---|---|
| `getId_eachType_matchesSpecTable` | `GENERIC` 0, `PROTOCOL_ERROR` 1, `UNSUPPORTED_VERSION` 2, `TIMEOUT` 3, written as literals with a `// spec:` comment like `NamespacedIdTest` | fails when a constant's id or order drifts from the spec table |
| `fromId_eachSpecId_returnsType` (parameterized 0 to 3) | `fromId(getId())` returns the same constant | fails when the lookup maps an id to the wrong constant |
| `fromId_unknownId_throws` (parameterized -1, 4, `Integer.MAX_VALUE`) | throws `IllegalArgumentException` naming the id | fails when an unknown id silently maps to a constant |

`DisconnectInfoTest` (`./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.DisconnectInfoTest' --rerun`):

| Test | Behaviour | Red case |
|---|---|---|
| `of_validArguments_returnsThem` | getters return type and reason | fails when a getter returns the wrong field |
| `of_emptyReason_accepted` | `""` is a valid reason | fails when an empty server reason is rejected |
| `of_nullType_throws`, `of_nullReason_throws` | `NullPointerException` | fails when a `null` slips into the value |
| `equals_sameFields_equalWithSameHashCode` | equal fields, equal objects and hashes; a different field, not equal | fails when equality ignores a field |
| `toString_containsFields` | type and reason appear | fails when `toString` drops a field |

`ClientListenerTest` (added at implementation; the JaCoCo gap check showed the `default` bodies
are reachable lines):

| Test | Behaviour | Red case |
|---|---|---|
| `ClientListenerTest#onConnect_notOverridden_doesNothing`, `#onDisconnect_notOverridden_doesNothing` | a listener overriding nothing ignores both events | fails when a default throws |

`EndpointListenerTest` is deleted with D8.

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: no wire or rule change; D7 is the client's reading of an unknown type, not a spec rule |
| New or changed public type or method in `client-api` | Javadoc on that type | Update: `Client`, `ClientListener`, `ClientBuilder`, `EndpointHandle#unregister`, `DisconnectInfo`, `DisconnectType`, `package-info` |
| A design decision, layer rule or convention | `CLAUDE.md` | Update `client-api/CLAUDE.md`: keep the D1/D2 and D5/D6 lines (the latter adds "or on the `ClientBuilder` before connecting"); drop the D8 line; extend "`Client` has no `connect()` and no settings; an implementation's builder creates it" with "and implements `ClientBuilder` (`addListener`, `connect()`); every other builder setting is the implementation's (BD-0005 D10)". Root `CLAUDE.md` § API design decisions, the ADR 0018 line: add "which implements `client-api`'s `ClientBuilder`", linking BD-0005 D10 |
| ADR the decision touches | `docs/decisions/0018-...` | N/A: D10 decides what 0018 leaves open and contradicts nothing in its Decision; no ADR edit |

## Edge Cases
- **Server rejects the version at handshake:** `onConnect` never fires; `onDisconnect` with
  `UNSUPPORTED_VERSION`; with no further attempt the client closes and
  `whenReady()` fails naming that disconnect (D1, D4). A listener added only on the `Client`
  after `connect()` may miss this; one added on the builder does not (D10).
- **`close()` before the first connection:** `whenReady()` completes exceptionally with
  `IllegalStateException`; listeners get `onDisconnect` (`GENERIC`) if an attempt was in progress.
- **`close()` called twice, or from inside a listener or handler:** the second call does nothing;
  inside a callback it returns without waiting (D2).
- **`send` racing `close()` on another thread:** throws, or is accepted and dropped without a
  report (D3, BD-0004 D3).
- **Server sends `Disconnect` type 9:** `GENERIC`, server's reason kept (D7).
- **Listener added while connected:** gets no `onConnect`; reads `isConnected()` after adding (D5).
- **Listener throws:** reported by the implementation; other listeners still get the event; the
  connection is unaffected (D5).
- **Adding the same listener twice, removing an unknown one:** no effect, no exception (Approach 5);
  the same listener added on the builder and again on the client is one listener.
- **Builder listener removed later:** `Client#removeListener` removes it like any other (D10).
- **`connect()` twice, or `addListener` after `connect()` on a builder:** `IllegalStateException`
  (D10, question 3).
- **`connect()` with an unreachable server:** returns a client; the failure arrives as
  `onDisconnect` (`GENERIC`) on the builder's listeners (D10).

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
- `minimalCompletionStage` behaviour on `toCompletableFuture()` (returns a new, independent
  future) must be checked on JDK 25 at implementation, or a defensive copy used.
- `ClientBuilder` is a contract for every future builder: a later setting that every
  implementation shares (say, the server address) is tempting to add here, which would turn it
  into the common configuration surface ADR 0018 rejected. A new method on `ClientBuilder` needs
  its own decision.
- The endpoint availability design moves out (D8): until that issue lands, a caller learns an
  endpoint is unusable only from `send` throwing, as on `development` today.
