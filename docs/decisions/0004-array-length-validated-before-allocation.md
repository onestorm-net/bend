# 0004: Array lengths are validated before allocating

**Status:** Accepted
**Date:** 2026-09-16

## Context

`ProtocolUtil.readVarIntArray` and `AvailableEndpointsPacket.readEndpointArray` allocated
`new int[length]` / `new Endpoint[length]` straight from a peer-supplied VarInt length prefix,
before checking that the buffer held that much data. A tiny frame, well under the frame cap,
could claim close to `Integer.MAX_VALUE` elements and trigger a multi-gigabyte allocation. The
likely result is an `OutOfMemoryError`, an `Error` that the decoder's `catch (Exception e)`
blocks don't catch.

Both directions were exposed: `AvailableEndpointsPacket` (server to client) and
`SupportedEndpointsPacket` (client to server, via `readVarIntArray`); the arrays are defined in
[protocol-v1.md § Available Endpoints and § Supported Endpoints](/protocol/protocol-v1.md). `ProtocolUtil.readString`
already guarded against this (`buffer.isReadable(length)` before reading); the array readers
never got the same check.

## Decision

`ProtocolUtil.validateArrayLength(buffer, length)` rejects a negative length, and a length
greater than `buffer.readableBytes()`, with `ProtocolException`. Every element (VarInt or
`Endpoint`) takes at least one byte, so readable bytes are a valid upper bound for any element
type. `readVarIntArray` and `readEndpointArray` call it before allocating.

## Does not decide

- Tighter per-type bounds (bytes per element). The one-byte bound is enough to stop the
  allocation; anything stricter is a separate decision.
- How a future reader is written. A new reader that allocates from a peer-supplied length has
  the same exposure; the check it needs is that reader's own decision.

## Rejected

None recorded.
