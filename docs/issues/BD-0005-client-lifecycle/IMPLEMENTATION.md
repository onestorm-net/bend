# Implementation Log

## 2026-11-06: client-api lifecycle contract

**Key Changes:**
- Created `DisconnectType.java`, `DisconnectOrigin.java`, `DisconnectInfo.java`,
  `ConnectionListener.java` in `client-api/src/main/java/net/onestorm/bend/client/api/`.
- Modified `Client.java`: `extends AutoCloseable`; `isConnected()`, `firstConnection()`,
  `addConnectionListener`, `removeConnectionListener`, `close()`; "or the client is closed" on both
  `register*` `@throws`; lifecycle paragraph and listener example in the class Javadoc.
- Modified `EndpointHandle.java` (`unregister` does nothing after close) and `package-info.java`.
- Tests: `DisconnectTypeTest`, `DisconnectInfoTest`, `ConnectionListenerTest`.
- `client-api/CLAUDE.md`: two rule lines (D1, D2; D5, D6).
- Kendo BD-0005: acceptance criteria rewritten to D1 to D7, with the close/send criterion marked
  as changed (D3).

**Learnings:**
- JaCoCo counts the empty bodies of `ConnectionListener`'s `default` methods as lines. They are
  reachable (a listener that overrides only one method runs the other), so they get a real test of
  the documented "does nothing by default" contract rather than an exclusion.

**Deviations from plan:**
- Added `ConnectionListenerTest` (`onConnect_notOverridden_doesNothing`,
  `onDisconnect_notOverridden_doesNothing`), not in the Testing Strategy; found by the JaCoCo gap
  check. Red case: a default method that throws.

**Branch review fixes:**
- `firstConnection()` Javadoc and D4: the failure message also covers a client closed before any
  attempt ended, when there is no last disconnect to name.

**Follow-ups noted but not done (client-netty epic obligations, from the branch review):**
- The `Disconnect` handler catches only `DisconnectType.fromId`'s `IllegalArgumentException` and
  reports `SERVER`/`GENERIC` with the server's reason; test it with types 4 and -1 (D7).
- `firstConnection()` returns a stage a caller can't complete (a `minimalCompletionStage`, not the
  internal `CompletableFuture`); test that `toCompletableFuture().complete(null)` by one caller
  doesn't complete it for others (D4).
- Plus what the plan already assigns to BD-0006 (listener thread and ordering) and the client-netty
  epic (behaviour tests, closing on give-up).

## 2026-11-06: PR #9 review, endpoint availability and listener naming

**Key Changes:**
- Created `EndpointListener.java` (D8) and `EndpointListenerTest.java`.
- `EndpointHandler` extends `EndpointListener<IncomingEndpoint<T>>`; `EndpointHandle#isAvailable()`;
  `Client#registerOutgoing(id, codec, listener)` overload; one sentence in `OutgoingEndpoint#send`
  tying its state refusals to `isAvailable()`.
- `firstConnection()` renamed `whenReady()` (D9); `ConnectionListener` renamed `ClientListener`,
  `add/removeConnectionListener` renamed `addListener`/`removeListener` (D5 revised); test renamed
  `ClientListenerTest`.
- `client-api/CLAUDE.md`: endpoint listener rule line; client listener line reworded.

**Learnings:**
- `EndpointListener` is generic over the handle (`H extends EndpointHandle`) so one interface serves
  both directions and each callback can use its own endpoint while the registration is still
  being created (the caller's variable isn't assigned yet inside the listener).
- `EndpointHandler` keeps a single abstract method, so existing lambdas and method references
  still compile; `EndpointListenerTest` pins that.

**Deviations from plan:**
- None beyond the review decisions, which PLAN.md and DECISIONS.md now carry.

**Follow-ups noted but not done (client-netty epic):**
- #4 (track the acknowledged set) fires `onAvailable`/`onUnavailable` and backs `isAvailable()`;
  #6 (send) must refuse from the same state so the two never disagree.

**Branch review fixes (round 2):**
- `EndpointHandle#isAvailable` and `EndpointListener`: a server-removed incoming endpoint becomes
  unavailable only after the client acknowledges the list; in-flight messages still reach the
  handler (§ Endpoint list updates). Outgoing becomes unavailable when the list arrives.
- `DisconnectOrigin.CLOSED`: only for an attempt that `close()` ended; giving up on its own emits no
  `CLOSED` disconnect.
- BD-0004 D1: pointer that its `isAvailable()` rejection is superseded in part by BD-0005 D8.
- Repeated client-netty obligations (unknown-type catch, minimal stage) stay as logged above.
