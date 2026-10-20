# 0001: Frame length prefix is a 28-bit VarInt with a separate configurable cap

**Status:** Accepted

## Context

`VarInt28FrameDecoder` and `VarInt28LengthEncoder` were adapted from Velocity's
`MinecraftVarintFrameDecoder`. The decoder's VarInt reader came with a hard 3-byte / 21-bit cap,
taken from vanilla Minecraft's packet-size limit, so the largest length it could read was
`2^21 - 1` (about 2MB). [protocol-v1.md § Frame](../../protocol/protocol-v1.md) allows frames up
to 4MB, and the encoder used the general 32-bit `ProtocolUtil.writeVarInt`, so the encoder could
write lengths the decoder refused to read back.

VarInts grow in 7-bit steps, so after 21 bits (under 4MB) the next width is 28 bits. No
standard VarInt lands on exactly 4MB.

The small-buffer read path (`readRawVarIntSmallBuf`, used with fewer than 4 readable bytes)
also truncated silently when its last byte still had the continuation bit set, handing back a
shorter, wrong length: a framing desync on partial reads. It can't throw there, because a third
continuation byte may be a 4-byte VarInt still waiting for its fourth byte.

The same audit (2026-09-16) documented two related facts in the spec: the frame `Length` field
is a narrower 4-byte / 28-bit VarInt than the general 5-byte / 32-bit `VarInt` type, and the
general VarInt sends its least-significant 7-bit group first.

## Decision

- The frame length VarInt reader decodes up to 4 bytes / 28 bits. The width stays in the class
  names (`VarInt28...`) because it is a hardcoded ceiling of the reader, not a setting.
- `MAXIMUM_FRAME_LENGTH` (`2^28 - 1`) is that wire ceiling, a constant on both classes.
- The size policy is a separate value: both classes take `maximumFrameLength` as a constructor
  argument, clamped to `Math.min(requested, MAXIMUM_FRAME_LENGTH)`. There is no no-arg
  constructor and no default policy; the caller always passes one.
- The decoder enforces the cap with an explicit `length > maximumFrameLength` check before any
  buffer is allocated. Reading the VarInt is O(1) whatever its value.
- The encoder enforces the same cap on encode, so an oversized outgoing frame fails locally
  instead of producing a length no `VarInt28FrameDecoder` can read.
- The small-buffer path resets and waits for more data when its last byte has the continuation
  bit set. Once four bytes are readable the fast path takes over and throws if the VarInt is
  wider than 28 bits.

## Does not decide

- Which cap a deployment passes. 4MB is the spec's limit; `MAXIMUM_FRAME_LENGTH` means "no cap
  beyond the wire format". The choice belongs to whatever module owns configuration.
- The width of the general `VarInt` type, which stays 32-bit.

## Rejected

- Keeping 21 bits: undershoots the spec's 4MB.
- Letting the VarInt width double as the size policy: the two are independent, and conflating
  them is what produced the original encoder/decoder asymmetry.
- A no-arg constructor with a default cap: bakes a business limit into the codec.
- Throwing in the small-buffer path on a third continuation byte: it can't tell a too-wide
  VarInt from an incomplete one.
