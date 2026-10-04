# Sins of omission

Hunt what the change obligates but does not contain: an absence is a finding. Flag
obligations this diff creates, not a recital of the list.

- **The symmetric operation**: `encode` obligates `decode` and the reverse; register obligates
  unregister; retain obligates release; a scheduled timeout obligates its cancel; a listener or
  handler added obligates its removal; a new outbound packet obligates the peer-side handler
  that consumes it, and a new endpoint obligates both its `registerOutgoing` and
  `registerIncoming` halves where the flags say both directions. The compiler cannot see a
  packet that one side sends and the other never handles. Name the missing half and what
  accumulates without it.
- **Exhaustiveness over the new case**: a new packet id, `Disconnect` `Type`, connection state,
  or direction flag obligates every switch, `ProtocolV1` registration, handler, and spec table
  over that set. Grep them all; say what an unhandled one does.
- **The covering test**: new observable behaviour obligates a test that observes it; a bug fix
  obligates a regression test that reproduces the symptom. Two ways a test that cannot fail
  still passes: a faked boundary sits between the assertion and the defect, so the defective
  path never executes; or a matcher is loose enough to accept the wrong value. Name what the
  suite is blind to. Faked boundaries worth naming: a decoder tested by calling `decode()`
  with the whole frame in one buffer, so the partial-read path never runs; an
  `EmbeddedChannel` whose pending tasks or scheduled timers are never run; a round-trip test
  where the same buggy helper both writes and reads, so encoder and decoder agree on the wrong
  layout; an assertion on "throws" that does not pin `ProtocolException` vs
  `IllegalArgumentException`. Verify a behaviour-preserving change against the layer that
  broke, never against the new code's own fake. JaCoCo attests execution, not this.
- **The promised behaviour**: text that names a system action obligates the implementing
  path. `protocol-v1.md` is the main one: a MUST, a disconnect condition, a timeout, a
  `Disconnect` `Type` it documents. Also Javadoc on `protocol` / `client-api`, the README, and
  skill docs. Check sentences in the diff, and sentences elsewhere that this diff's code change
  made false. Grep for them; an unkept promise is a finding.
- **The peer already in place**: a wire-format change obligates a protocol version decision.
  A peer built against the previous layout is already deployed; say what it does with the new
  bytes (misparses silently, or fails the handshake cleanly).
- **The operational trail**: a new failure mode obligates a signal someone can act on (a
  `Disconnect` with a `Reason` that names the cause, a `ProtocolException` message specific
  enough to debug from); a new entry point obligates its guards; a new configuration knob
  (a cap, a timeout) obligates its documented default and a constructor or builder that
  requires it explicitly, the way `maximumFrameLength` has no no-arg constructor.

State the consequence of the absence.
