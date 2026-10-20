# 0003: Zero-length frames are rejected; canonical VarInts are not required

**Status:** Accepted
**Date:** 2026-09-16

## Context

A length VarInt can decode to `0` through a non-minimal encoding such as `0x80 0x00`. That
starts with a non-`0x00` byte, so the NUL padding skip ([ADR 0002](/docs/decisions/0002-nul-padding-skip.md))
never sees it. The decoder's old `if (length > 0) { ... }` guard silently did nothing for it.

The same audit documented two framing facts in the spec: a packet's fields consume exactly
`Length` bytes (no trailing bytes), except `Endpoint Message`, whose payload consumes the
remainder; and the Packet ID is a single byte (0 to 255).

## Decision

- A frame whose length decodes to `0` raises `ProtocolException`, like the decoder's other
  framing violations. [protocol-v1.md § Frame](/protocol/protocol-v1.md) states it as a MUST.
- VarInt reading stays tolerant of non-canonical encodings everywhere, as in the Velocity and
  Minecraft code this was adapted from. The `length == 0` check is the guard for the frame case.

## Does not decide

- Revisit canonical encoding only if a concrete reason to require it appears.

## Rejected

- Requiring canonical (minimal-length) VarInts, rejecting over-long encodings in the reader via
  `ProtocolUtil.varIntLength(value)`: it would close the class of issue protocol-wide, but it
  reworks VarInt decoding everywhere for one frame case, and it would make the `length == 0`
  check unreachable.
