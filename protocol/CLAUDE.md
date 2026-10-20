# protocol

The wire format: types, packets, the frame codec and the protocol table (`ProtocolV1`). The
specification is [protocol-v1.md](protocol-v1.md); code and spec change together, in the same
task. Rules here are one line each; the linked ADR holds the reasoning.

## Frame codec

`codec/VarInt28FrameDecoder` and `codec/VarInt28LengthEncoder` were adapted from Velocity's
`MinecraftVarintFrameDecoder` (https://github.com/PaperMC/Velocity).

- The frame length reader is 28 bits wide; `MAXIMUM_FRAME_LENGTH` is that ceiling, and the size
  policy is the separate `maximumFrameLength` constructor argument, clamped to it, with no
  default. ([0001](../docs/decisions/0001-frame-length-28-bit-varint.md))
- The encoder enforces the same cap, so it never writes a length the decoder can't read.
  ([0001](../docs/decisions/0001-frame-length-28-bit-varint.md))
- Leading `0x00` bytes are skipped; more than 16 in a row disconnects.
  ([0002](../docs/decisions/0002-nul-padding-skip.md))
- A frame length of `0` raises `ProtocolException`; non-canonical VarInts are still accepted.
  ([0003](../docs/decisions/0003-zero-length-frames-rejected.md))

## Reading untrusted input

- `readVarIntArray` and `readEndpointArray` check their peer-supplied length with
  `ProtocolUtil.validateArrayLength` before allocating.
  ([0004](../docs/decisions/0004-array-length-validated-before-allocation.md))
- `Endpoint` validates its flags and id in the constructor, for `Endpoint.of` and wire decode.
  ([0005](../docs/decisions/0005-endpoint-validation.md))
- Malformed or oversized wire input raises `ProtocolException` (no stack trace by default);
  caller misuse at protocol-definition time raises `IllegalArgumentException`.
  ([0011](../docs/decisions/0011-protocol-exception.md))

## Tests and coverage

- A line the coverage report shows as missed is a question of reachability: test it if
  reachable, delete it if not, never cover it with a contrived test. ([0009](../docs/decisions/0009-unreachable-checks-deleted.md),
  [0010](../docs/decisions/0010-coverage-reporting.md))
- Run: `./gradlew :bend-protocol:test --rerun`; report at
  `protocol/build/reports/jacoco/test/jacocoTestReport.xml` (and `.csv`).
