# BD-0005-client-lifecycle: Decisions

## D1: A client is connecting, connected or closed; giving up for good means closed
**Status:** Accepted

**Context:** Reconnect policy is client-netty's (issue scope), but the contract must say what a caller sees when the implementation stops trying, or `firstConnection()` (D4) can stay pending forever.

**Decision:** Between connection attempts the client is not connected; once the implementation will not try again (no reconnect configured, or its policy gave up), it closes itself exactly as `close()` would. So a client is either still trying or closed, never stuck.

**Rejected:**
- A separate "gave up" state beside closed: a client that will never connect again has nothing left a caller can do with it except what closed already allows.

## D2: `close()` is final, idempotent and does not wait for I/O
**Status:** Accepted

**Context:** The issue makes `close()` final. A caller may call it from an endpoint handler or a connection listener, which may run on the I/O thread (BD-0006).

**Decision:** `Client extends AutoCloseable` with `void close()` (no checked exception). After it returns, `registerIncoming`, `registerOutgoing` and `addConnectionListener` throw `IllegalStateException`; `send` already does (BD-0004 D2); `EndpointHandle#unregister` does nothing; no handler is called for a message that arrives later. Calling it again does nothing. It does not wait for the socket to close, so calling it on the I/O thread can't deadlock. The client sends no packet: protocol-v1.md has no client-to-server `Disconnect`.

**Rejected:**
- Waiting until the channel is closed: deadlocks when called from the I/O thread; resource release timing is BD-0006 and client-netty's.

## D3: The issue's "sends fail their future" criterion follows BD-0004
**Status:** Accepted

**Context:** BD-0005's issue was written when `send` returned a future. BD-0004 made it `void send(T)` that throws a plain `IllegalStateException` for every refused state, closed included (BD-0004 D1, D2), and drops an accepted message without a report when the state changes before the write (D3).

**Decision:** The criterion becomes: after `close()` returns, `send` throws `IllegalStateException`; a `send` racing `close()` on another thread either throws or is accepted and then dropped without a report. No change to `OutgoingEndpoint`; the Kendo issue is updated to say so.

**Rejected:**
- Reintroducing a per-send future for the close race: BD-0004 D1 rejected per-message results.

## D4: `firstConnection()` returns a `CompletionStage<Void>`
**Status:** Accepted

**Context:** The issue asks for "an optional way to wait for the first successful connection". A caller may be on a user thread (can block) or in a handler on the I/O thread (must not).

**Decision:** `CompletionStage<Void> firstConnection()`: completes normally the first time the client becomes connected and never resets; completes exceptionally with an `IllegalStateException` naming the last disconnect (or saying none had ended yet) if the client is closed before it ever connected (D1). A caller blocks with `toCompletableFuture().get(timeout)`. The implementation returns a stage the caller can't complete (verify `CompletableFuture#minimalCompletionStage` at implementation).

**Rejected:**
- `boolean awaitConnected(Duration)`: a blocking-only API that a handler on the I/O thread could call and deadlock on.
- Returning `CompletableFuture<Void>`: any caller could `complete()` it for every other caller.

## D5: Connection events go to a `ConnectionListener` added on the client
**Status:** Accepted

**Context:** Callers want connect and disconnect events, also after reconnects. ADR 0018 keeps settings in the builder, but code that only holds a `Client` must be able to listen.

**Decision:** `ConnectionListener` with `default void onConnect()` and `default void onDisconnect(DisconnectInfo info)`; `Client#addConnectionListener` / `removeConnectionListener`. Every connection attempt ends with exactly one `onDisconnect`, including attempts that never connected (refused, or a server `Disconnect` in reply to `Handshake`); `onConnect` fires in between only when the attempt connected. Events are delivered in order; no replay for a listener added later. A throwing listener is reported by the implementation, mirroring `EndpointHandler#handle`.

**Rejected:**
- Listeners only in the builder: code holding just a `Client` couldn't listen.
- `addConnectionListener` returning a handle with `remove()`: a type that exists only to remove; add/remove by identity is enough.

## D6: `DisconnectInfo` carries origin, type and reason, all always present
**Status:** Accepted

**Context:** The issue wants a server-sent `Type` and `Reason`, but a connection also ends because the client gave up (protocol-v1.md § Client-side disconnect conditions), the socket failed, or `close()` was called.

**Decision:** Final class `DisconnectInfo.of(DisconnectOrigin, DisconnectType, String reason)` with getters, `equals`, `hashCode`, `toString`. `DisconnectOrigin` is `SERVER`, `CLIENT`, `CONNECTION` or `CLOSED`. For `SERVER` type and reason are the server's; for `CLIENT` the type is `TIMEOUT` or `PROTOCOL_ERROR`; for `CONNECTION` and `CLOSED` it is `GENERIC`. One switch on the type works for every origin.

**Rejected:**
- An `Optional<DisconnectType>` present only for `SERVER`: forces every caller to handle the empty case for the common branch (retry on timeout).
- A `Throwable` cause for `CONNECTION`: nothing reads it yet; a getter can be added later without breaking callers.

## D7: `DisconnectType` mirrors the spec table; an unknown wire type reads as `GENERIC`
**Status:** Accepted

**Context:** `DisconnectPacket#decode` reads `Type` as any VarInt and does not check it, so a server can send a type this client doesn't know (ADR 0007 leaves adding types open).

**Decision:** `enum DisconnectType { GENERIC, PROTOCOL_ERROR, UNSUPPORTED_VERSION, TIMEOUT }` with `getId()` (0 to 3) and `static fromId(int)`, which throws `IllegalArgumentException` for any other id. client-netty reports a server `Disconnect` with an unknown type as origin `SERVER`, type `GENERIC`, the server's reason kept, by catching `fromId`'s `IllegalArgumentException` so the 0 to 3 range lives only in `DisconnectType`: the spec's type 0 means "See Reason", and the connection is ending anyway.

**Rejected:**
- `fromId` returning `GENERIC` for unknown ids: hides a caller's mistake in a public method.
- Treating an unknown type as a protocol error (origin `CLIENT`): loses the server's reason for no action the caller could take.
