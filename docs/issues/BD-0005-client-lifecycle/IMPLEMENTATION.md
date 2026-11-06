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
