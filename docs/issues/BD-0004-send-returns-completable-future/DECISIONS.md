# BD-0004-send-returns-completable-future: Decisions

## D1: `send` returns `void` and throws what the client can check
**Status:** Accepted

**Context:** The protocol has no per-message response, so a returned future could only report "written to the socket", never "the server handled it".

**Decision:** `void send(T message)`. Every failure the client can check at the call throws from `send` (D2); anything after it returns is not reported per message (D3).

**Rejected:**
- `CompletableFuture<Void>` (the first version of this plan): suggests a delivery result the protocol can't give.
- An `isAvailable()` query beside `send`: a check-then-send races a disconnect anyway; add one when a caller needs it.

## D2: Every refused state throws a plain `IllegalStateException`
**Status:** Accepted (revised at review, 2026-11-06)

**Context:** `send` throws for states a reconnecting client passes through and for the caller's own lifecycle calls (unregister, close).

**Decision:** A plain `IllegalStateException`, its message naming the reason, when this handle is unregistered, the server doesn't offer the id, its flags don't allow client to server, the declaring `Supported Endpoints` isn't acknowledged, or the client is disconnected or closed; `send(null)` throws `NullPointerException`. ADR 0011 and BD-0003 D6: a call the current state forbids. ADR 0011 splits exception types only when a caller needs to catch them apart, and nothing catches yet; a subclass can be added later without breaking a caller, removing one can't.

**Rejected:**
- `EndpointUnavailableException extends IllegalStateException` for the endpoint and connection states (the first version of D2): no caller needs it yet, and "closed" and "unregistered" are both the caller's doing yet threw different types.
- Extending `RuntimeException`: departs from ADR 0011's state rule.

## D3: Nothing after `send` returns is reported per message
**Status:** Accepted

**Context:** The connection or the endpoint can change after the check passed but before the write, and a write can fail on the socket.

**Decision:** An accepted message may still be dropped, without a report; the Javadoc gives a state change before the write and a failed write as examples, not a closed list, so client-netty may also drop at its bound on pending writes. A failed write is the connection's problem and surfaces through BD-0005's connection state, not through `send`.

**Rejected:**
- A per-message callback or future for write failures: the D1 objection again.

## D4: When `encode` runs is BD-0006's
**Status:** Accepted

**Context:** Codec failures and a payload above the frame cap can only throw from `send` if `encode` runs before `send` returns; that is a threading choice.

**Decision:** BD-0004 promises nothing about when `encode` runs. Throwing codec failures from `send`, and an `IllegalArgumentException` for an encoded message above the frame cap (the developer's choice once encoding is synchronous), move to BD-0006.

**Rejected:**
- Deciding here that `encode` runs inside `send`: the developer keeps all thread placement in BD-0006.
