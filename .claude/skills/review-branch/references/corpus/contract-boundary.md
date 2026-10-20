# Contract & boundary integrity

Data is only as good as the check at the crossing. Two censuses over the boundaries this
diff adds or moves.

**Boundary validation.** List every site where data from outside the process enters typed
code: a packet's `decode` reading a `ByteBuf`, a `ProtocolUtil` reader, an `Endpoint` built
from wire fields, a user payload decoded by an `EndpointCodec`, a configuration value handed to
a codec constructor. For each site, cite the line that checks the value before the first use:
the length against readable bytes, the id against its format, the flags against "at least one
direction bit", the enum-like code against its known set. A value used with no check between
the read and the use is the finding. Say what happens on a mismatch: fail closed with a
`ProtocolException`, or garbage propagating downstream wearing the type, and name the first
consumer that acts on the wrong value. A check enforced in the constructor counts only when
every construction path (factory method and wire decode alike) goes through it.

**Spec↔code and encode↔decode drift.** List every pair where one artifact declares a shape and
another produces or consumes it:

- a packet's `encode` beside its `decode`: field order, field widths, VarInt vs fixed int,
  string length rule, what `encode` writes that `decode` never reads
- an encoder beside its decoder across the wire: `VarInt28LengthEncoder` and
  `VarInt28FrameDecoder` must agree on width and cap (the 21-bit vs 32-bit asymmetry shipped
  once)
- `protocol-v1.md` beside the code: packet ids, field layouts, the MUST rules, the disconnect
  conditions on both sides, the `Disconnect` `Type` values, the timeout constants
- the client's view of the flow beside the server's: who sends what in which state, and the
  `availableRevision` / `supportedRevision` each side echoes back

Diff each pair field by field. A field one side has and the other lacks, a width or
signedness the two sides disagree on, a rule one side enforces and the other never applies:
name which side is wrong and what the disagreement silently accepts or drops. When code and
spec disagree, do not assume the code is right; the spec is the published contract.
