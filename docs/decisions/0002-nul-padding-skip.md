# 0002: NUL padding before a frame is skipped

**Status:** Accepted

## Context

The frame decoder inherited `FIND_NON_NUL` from Velocity: it skips runs of `0x00` before reading
a frame's length prefix, and disconnects after more than 16 in a row. In Velocity this tolerates
legacy Minecraft client and proxy noise, which doesn't apply to Bend.

The behaviour still holds up on its own: every Bend frame carries at least a 1-byte packet id,
so a frame length is never legitimately `0`, and a leading `0x00` can only be padding.

## Decision

Readers skip `0x00` bytes before a frame's length prefix, and a run of more than 16 consecutive
NUL bytes disconnects. The rule text is in [protocol-v1.md § Frame](/protocol/protocol-v1.md).
The threshold of 16 is kept as inherited.

## Does not decide

- Whether writers may emit padding. The rule constrains readers.

## Rejected

None recorded.
