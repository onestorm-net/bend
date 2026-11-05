# BD-0004-send-returns-completable-future: Decisions

## D1: `send` returns `void` and throws what the client can check
**Status:** Accepted

**Context:** The protocol has no per-message response, so a returned future could only report "written to the socket", never "the server handled it".

**Decision:** `void send(T message)`. Every failure the client can check at the call throws from `send` (D2); anything after it returns is not reported per message (D3).

**Rejected:**
- `CompletableFuture<Void>` (the first version of this plan): suggests a delivery result the protocol can't give.
- An `isAvailable()` query beside `send`: a check-then-send races a disconnect anyway; add one when a caller needs it.

## D2: Unusable state throws `EndpointUnavailableException`, an `IllegalStateException`
**Status:** Accepted

**Context:** `send` now throws for states a reconnecting client passes through, and for the caller's own mistakes.

**Decision:** `EndpointUnavailableException extends IllegalStateException` when the server doesn't offer the id, its flags don't allow client to server, the declaring `Supported Endpoints` isn't acknowledged, or the client is disconnected or closed. A send on this handle after its own `unregister()` throws a plain `IllegalStateException`, checked before the other cases; `send(null)` throws `NullPointerException`. Fits ADR 0011 and BD-0003 D6: a call the current state forbids.

**Rejected:**
- Plain `IllegalStateException` for all: a tolerant caller can't tell "disconnected" from its own unregistered handle.
- Extending `RuntimeException`: departs from ADR 0011's state rule.
- A separate `NotConnectedException`: one more public type for a difference the message already names.

## D3: Nothing after `send` returns is reported per message
**Status:** Accepted

**Context:** The connection or the endpoint can change after the check passed but before the write, and a write can fail on the socket.

**Decision:** Such a message is dropped silently; a failed write is the connection's problem and surfaces through BD-0005's connection state, not through `send`. The Javadoc says returning means accepted, not written or received.

**Rejected:**
- A per-message callback or future for write failures: the D1 objection again.

## D4: When `encode` runs is BD-0006's
**Status:** Accepted

**Context:** Codec failures and a payload above the frame cap can only throw from `send` if `encode` runs before `send` returns; that is a threading choice.

**Decision:** BD-0004 promises nothing about when `encode` runs. Throwing codec failures from `send`, and an `IllegalArgumentException` for an encoded message above the frame cap (the developer's choice once encoding is synchronous), move to BD-0006.

**Rejected:**
- Deciding here that `encode` runs inside `send`: the developer keeps all thread placement in BD-0006.
