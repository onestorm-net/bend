# 0012: No Netty types in public API modules

**Status:** Accepted

## Context

`io.netty.buffer.ByteBuf` is reference-counted with a manual `retain()`/`release()` lifecycle.
In an API module it would:

1. force that lifecycle on every API consumer, including ones who never chose Netty;
2. give anyone implementing the API on another transport (plain NIO, a test double, a future
   non-Netty client) a hard dependency on Netty;
3. tie the API's versioning to Netty's breaking changes, even when the protocol didn't change.

The client code from before the Gradle migration did this: `EndpointCodec` in `client-api` took
a `ByteBuf`, which pulled `netty-all` into `client-api`'s build.

## Decision

- `client-api` (and `server-api`, once it exists) expose no Netty types: no `ByteBuf`,
  `Channel`, `ChannelFuture` or `EventLoop` in a public signature.
- Payloads cross the API boundary as `java.nio.ByteBuffer`. Conversion to and from `ByteBuf`
  happens once, at the boundary, inside the `-netty` implementation modules.
- The client rework redoes `EndpointCodec` on `java.nio.ByteBuffer` rather than patching the
  old code in place.

## Does not decide

- Copy versus view semantics at the `ByteBuffer` boundary; each feature decides and documents
  its own.

## Rejected

- Exposing `ByteBuf` for zero-copy convenience: the three costs above fall on every consumer.
- Patching the pre-migration `EndpointCodec` in place: the client is being redone as tracked
  issues instead.
