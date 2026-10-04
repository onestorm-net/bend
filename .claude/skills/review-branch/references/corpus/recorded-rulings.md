# Recorded rulings

The repository's front door records rulings: decisions already made, with the reasoning behind
them and, often, the alternative that was considered and declined. The front door is
authoritative over Javadoc, code comments, and the diff's own account of itself. Bend's front
door is two files:

- `CLAUDE.md` at the repo root: design decisions and their history ("Frame decoder", "Protocol
  architecture", "Testing / coverage", "Module naming convention", "API design decisions",
  "Module set", and the rest)
- `protocol/protocol-v1.md`: the wire spec. Its MUST / MUST NOT rules, disconnect conditions,
  field layouts, and constants are rulings of the same weight

Open both, in full. The front door's rulings are the authoritative list of standing rules; do
not work from memory of which rules exist. A file either of them merely links (a skill, a
README) is not a ruling home. In `checked`, name the two files and the headings you checked
against.

## Standing rulings to know before you start

These are the rulings diffs most often brush against. They are a starting point, not the full
set: the front door is.

- **Unreachable defensive checks are deleted, not kept.** A guard the current code cannot
  reach (the decoder's old `length < 0` check, the empty-buffer guard in
  `readRawVarIntSmallBuf`) is removed; it comes back with a real test at the point a change
  makes it reachable. A diff that adds a "just in case" check with no producer, or a contrived
  test (reflection, an impossible hand-built state) to turn one green, contradicts this.
- **`ProtocolException` for wire input, IAE/ISE for caller misuse.** Every malformed or
  oversized-input throw site in `protocol` uses `ProtocolException` (one flat class, stack
  trace skipped unless `setDebugStackTraces(true)`). Setup-time misuse
  (`AbstractProtocol.getPacketId` for an unregistered class, `registerPacket` with an
  out-of-range id) stays `IllegalArgumentException` on purpose.
- **No `io.netty.buffer.ByteBuf` (or any Netty type) in `client-api` or the future
  `server-api`.** `java.nio.ByteBuffer` at the API boundary, converted once inside the
  `-netty` modules. `client-api` depends on nothing.
- **Non-canonical VarInt encodings are tolerated, deliberately.** Requiring minimal-length
  VarInts was considered and declined; the `length == 0` frame check guards the one case that
  mattered. A diff that starts rejecting over-long VarInts reverses a ruling.
- **The 16-NUL padding threshold.** Frames may be preceded by `0x00` padding, which readers
  skip; more than 16 consecutive NULs disconnect. The number was inherited from Velocity and
  kept on purpose.
- **The frame cap is a constructor argument, clamped to the 28-bit ceiling.** No no-arg
  constructor, no default business cap baked into the codec; `MAXIMUM_FRAME_LENGTH` is the
  wire ceiling, `maximumFrameLength` the policy, and the two are not conflated.
- **Two send paths.** Built-in packets are written to the channel directly; user payloads go
  through registration lookup and `EndpointMessage`. One public `send` for both is the
  pre-migration bug.
- **No `server-api` module in 0.1, and no module system.** Registration and routing live in
  `server-netty` behind Netty-agnostic signatures. Ownership tracking and lifecycle belong to
  0.2+.
- **Supported endpoints are resent as a whole snapshot**, not a delta.
- **Module naming**: the contract gets the plain name (`client-api`), the implementation is
  named for its mechanism (`client-netty`), never `-impl`.

## The three shapes

List each ruling that names a behaviour this diff changes, then check the diff against it.
Cite the ruling by its file and heading (e.g. `CLAUDE.md` › API design decisions, or
`protocol-v1.md` › Frame). Three shapes:

- **The diff changes code against a ruling it does not touch.** The recorded rule stands
  and the change now contradicts it. File it: `introduced`, and `confirmed` when the
  contradiction is on the page. The ruling is the repository's decision; a test edited in
  the same diff to expect the new behaviour is part of the change, never a license for it.
  Say plainly which ruling the change reverses.
- **The diff rewrites a ruling while the code keeps the old behaviour.** The front door
  now misstates the code it governs, and every later reader inherits the error. A
  `protocol-v1.md` edit with no matching code change is the common case. File it:
  `introduced`.
- **The diff rewrites a ruling and the code together, consistently.** A rule change is
  allowed, and never silent. File the reversal with `confidence: unconfirmed` and a
  `proof_gap` naming what would authorize it (a `docs/issues/<branch-name>/DECISIONS.md` entry
  naming the reversal, or the `CLAUDE.md` entry recording the new decision and why). State the
  old rule, the new rule, and that ruling text, code, and tests moved together.

A disagreement this diff touched on neither side is ordinary `pre_existing` territory, not
this census.

**The branch's own prose.** When the work directory's `PLAN.md` exists, every factual claim it
makes about what the code does (especially in its security and cost surface section) is a
ruling of the same kind: check it against the diff. A contradicted safety or cost claim files
`introduced`, `confirmed`. A D-numbered entry in `DECISIONS.md` is a recorded choice: you may
file a finding against its factual basis, and you file the finding the choice would waive
anyway, quoting the D-line in `evidence`. The parent session weighs it. You never
apply the waiver yourself.
