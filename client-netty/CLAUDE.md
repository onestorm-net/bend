# client-netty

The Netty implementation of `client-api`. Rules here are one line each; the linked ADR holds
the reasoning.

- Built-in packets are written to the channel through an internal path, never through the
  public endpoint send path. ([0008](/docs/decisions/0008-two-protocol-layers.md))
- Convert between `ByteBuf` and `java.nio.ByteBuffer` here, at the API boundary.
  ([0012](/docs/decisions/0012-no-netty-types-in-api.md))
- The builder's `connect()` returns the `client-api` `Client` interface; the concrete client
  class is package-private. ([0018](/docs/decisions/0018-client-built-by-implementation-builder.md))
- No Javadoc: this is an implementation module. ([0021](/docs/decisions/0021-javadoc-scope.md))
