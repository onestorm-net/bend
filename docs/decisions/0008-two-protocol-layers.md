# 0008: Two protocol layers, two send paths

**Status:** Accepted

## Context

[protocol-v1.md](../../protocol/protocol-v1.md) has two kinds of traffic:

1. **Built-in packets**: `Handshake`, `Disconnect`, `Available Endpoints` and its
   acknowledgement, `Supported Endpoints` and its acknowledgement, `Keep Alive`. Their order and
   meaning are fixed by the spec's Flow.
2. **Endpoint messages**: arbitrary user payloads, each carried in an `Endpoint Message` packet
   and routed by endpoint id to whatever was registered for it.

The client code from before the Gradle migration sent `HandshakePacket` and `KeepAlivePacket`
through the same public `Client.send(Object)` meant for registered endpoint payloads. No
registration exists for a built-in packet type, so they were silently dropped.

## Decision

The layers stay separate in implementation code:

- Built-in packets are protocol-encoded and written to the channel through an internal path.
  They are never looked up in the endpoint registration table.
- User payloads go through the public send path, which requires a registration and wraps the
  payload in `Endpoint Message`.

## Does not decide

- The names or shape of either send path in `client-netty` or `server-netty`.

## Rejected

- One send path for both layers: built-in packets have no registration, so they are dropped
  (the pre-migration bug).
