# 0009: Unreachable defensive checks are deleted

**Status:** Accepted

## Context

Adding JaCoCo ([ADR 0010](0010-coverage-reporting.md)) left two lines in the frame decoder that
no input could cover. Both were traced rather than tested around:

- `if (length < 0) throw ...` in `decode()`. The VarInt reader caps at 28 bits
  ([ADR 0001](0001-frame-length-28-bit-varint.md)), which can never set an `int`'s sign bit,
  and neither read path returns a negative failure value: an incomplete read returns `0` with
  the reader index unmoved, and a too-wide VarInt throws. The same check sits verbatim in
  Velocity's `MinecraftVarintFrameDecoder`, equally dead against its 21-bit reader; it most
  likely came from a general 32-bit VarInt reader, where 5-byte encodings can produce a
  negative `int` (as `ProtocolUtil.readVarInt` here can, by design).
- `if (!buffer.isReadable()) return 0;` at the top of `readRawVarIntSmallBuf`. Netty's
  `ByteToMessageDecoder` only calls `decode()` while the buffer is readable, and `decode()`
  reaches the VarInt reader only after `FIND_NON_NUL` has found a non-NUL byte, so the small
  path always sees 1 to 3 readable bytes.

With both removed, `protocol` reached 100% line, branch and instruction coverage, verified in
the JaCoCo report.

## Decision

- A coverage gap that won't close is a prompt to ask "is this reachable at all", not a reason
  to write a contrived test (for example through reflection) to turn it green.
- A defensive check for a case the current code can't produce is deleted, not kept "just in
  case" and not excluded from coverage. It comes back, with a real test, in the change that
  makes the case reachable.

## Does not decide

- Checks on input that can reach them, however unlikely. Hostile wire input is reachable by
  definition.

## Rejected

- Keeping unreachable checks as future-proofing: there is no real test for them, only a
  contrived one, so they are coverage gaps that look like safety.
- A standing coverage exclusion for such lines.
