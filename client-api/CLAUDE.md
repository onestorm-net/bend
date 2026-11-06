# client-api

The public contract for writing code against a Bend client. Rules here are one line each; the
linked ADR holds the reasoning.

- Depends on nothing: not on `protocol`, not on Netty. It holds interfaces and its own small
  value types. ([0018](/docs/decisions/0018-client-built-by-implementation-builder.md))
- No Netty types in any public signature; payloads cross as `java.nio.ByteBuffer`.
  ([0012](/docs/decisions/0012-no-netty-types-in-api.md))
- `Client` has no `connect()` and no settings; an implementation's builder creates it.
  ([0018](/docs/decisions/0018-client-built-by-implementation-builder.md))
- `send` returns nothing and throws a plain `IllegalStateException` for every state the client can
  check that forbids the send; no exception subtype until a caller needs one.
  (BD-0004 D1, D2, [DECISIONS.md](/docs/issues/BD-0004-send-returns-completable-future/DECISIONS.md))
- `close()` is final and does not wait for I/O; afterwards registering and adding a listener throw
  `IllegalStateException`. An implementation that stops reconnecting closes the client.
  (BD-0005 D1, D2, [DECISIONS.md](/docs/issues/BD-0005-client-lifecycle/DECISIONS.md))
- Connection events go to a `ConnectionListener` on the `Client`; every connection attempt ends
  with one `onDisconnect` carrying a `DisconnectInfo`.
  (BD-0005 D5, D6, [DECISIONS.md](/docs/issues/BD-0005-client-lifecycle/DECISIONS.md))
- Public types and members get Javadoc. ([0021](/docs/decisions/0021-javadoc-scope.md))
- No Lombok, not even `compileOnly`: public members are written out so each carries its own
  Javadoc. (BD-0001 D8, [DECISIONS.md](/docs/issues/BD-0001-namespaced-id-value-type/DECISIONS.md))
