# BD-0004-send-returns-completable-future: Decisions

## D1: `send` returns `CompletableFuture<Void>`
**Status:** Accepted

**Context:** `send` must serve fire-and-forget callers and callers who handle the outcome, as JDA's `queue()` does with zero or two callbacks.

**Decision:** `CompletableFuture<Void> send(T message)`, one method. Ignoring the result is fire-and-forget; `whenComplete` handles success and failure in one callback. Each call returns a new future; completing or cancelling it does not affect the send.

**Rejected:**
- `CompletionStage<Void>`: guards against the caller completing its own future, but needs `toCompletableFuture()` for `join()`, and the issue names `CompletableFuture`.
- JDA-style overloads `send(T)` / `send(T, onSuccess, onFailure)`: careful code must rebuild a future by hand for timeouts or `join()`.
- An own `SendResult` type: a new public type to keep stable, which still needs `toFuture()`.

**Consequences:** Revisit once a first working prototype exists; the cleanup pass may change the shape.

## D2: `EndpointUnavailableException` carries only a message
**Status:** Accepted

**Context:** The exception that fails the future when the endpoint can't be used needs a shape; `client-netty` constructs it, so it needs a public constructor.

**Decision:** `public class EndpointUnavailableException extends RuntimeException` with one public constructor `(String message)`; the implementation names the id and the reason in the message. Unchecked, because it only ever arrives through the future, never from a `throws` clause.

**Rejected:**
- `NamespacedId getId()`: the caller holds the handle and knows the id; `NamespacedId` is not `Serializable`, so the field would be `transient` and `null` after deserialization.
- A reason enum (not available, disconnected, closed): nothing branches on it yet, and connection state is BD-0005's `isConnected()` and disconnect events.

## D3: Caller bugs throw, everything else fails the future
**Status:** Accepted

**Context:** The issue makes send-after-own-`unregister()` throw; it says nothing about `null` or a failing codec. BD-0002 already says "a failed encode fails the send's future".

**Decision:** `send(null)` throws `NullPointerException` and a send after this handle's `unregister()` throws `IllegalStateException`, both at the call (BD-0003 D6). If `EndpointCodec#encode` throws, the future fails with that exception; if it returns `null`, with a `NullPointerException`. `send` never blocks waiting for the write.

**Rejected:**
- Failing the future for `send(null)` too: a caller bug that a fire-and-forget caller would never see.

## D4: "Unusable" is broad, and no send stays pending past the connection
**Status:** Accepted

**Context:** The issue lists not available, disconnected and closed; protocol-v1.md § Endpoint list updates adds an unacknowledged cycle and a removal, and § Endpoint Message the direction flags.

**Decision:** The future fails with `EndpointUnavailableException` when the id is not in the current `Available Endpoints`, its flags don't permit client to server, its `Supported Endpoints` cycle is not yet acknowledged, the client is disconnected or closed, or this handle is unregistered after the send was accepted. A future completes when the message is written or fails; a write the connection never takes stays pending only until the connection closes, then fails. No write timer is promised: a server that keeps sending `Keep Alive` but stops reading can hold a write open.

**Rejected:**
- Only the issue's three cases: leaves the others to `client-netty` to decide by accident.

## D5: No test class in `client-api`
**Status:** Accepted

**Context:** The branch adds one interface method and an exception with a single constructor that only calls `super(message)`.

**Decision:** No test class, for BD-0003 D5's reason: no executable logic of its own, so a test cannot fail for any change to the contract. The behaviour is tested in `client-netty`.

**Rejected:**
- `EndpointUnavailableExceptionTest` checking `getMessage()`: tests `RuntimeException`.

## D6: A write failure's cause has no documented type
**Status:** Accepted

**Context:** A failed write fails the future with its cause, which at runtime can be a Netty subclass of a JDK exception, or `protocol`'s `ProtocolException` for a payload above the frame cap; neither module is visible to `client-api` callers.

**Decision:** The Javadoc says the future fails with the exception that caused the write to fail and promises no type for it; callers treat it as a `Throwable`. Only `EndpointUnavailableException` and the codec's own exception (D3) are named.

**Rejected:**
- Wrapping every cause in a `client-api` exception: one more type now, before the prototype shows anyone branches on it.
- Promising JDK types only: `client-netty` would have to translate Netty and `protocol` exceptions it does not control.
