# client-api

The public contract for writing code against a Bend client. Rules here are one line each; the
linked ADR holds the reasoning.

- Depends on nothing: not on `protocol`, not on Netty. It holds interfaces and its own small
  value types. ([0018](../docs/decisions/0018-client-built-by-implementation-builder.md))
- No Netty types in any public signature; payloads cross as `java.nio.ByteBuffer`.
  ([0012](../docs/decisions/0012-no-netty-types-in-api.md))
- `Client` has no `connect()` and no settings; an implementation's builder creates it.
  ([0018](../docs/decisions/0018-client-built-by-implementation-builder.md))
- Public types and members get Javadoc. ([0021](../docs/decisions/0021-javadoc-scope.md))
