# Security & cost surface

The two censuses below catch the worst findings this review produces. Run both wherever the
diff adds or moves a wire entry point or touches a guard on one. Bend's attacker is the peer:
a malicious server against a client and a malicious client against a server are both in scope,
and every byte off the socket is theirs to choose.

**Entry-point census.** First list every wire entry point the diff adds or moves:

- a new or changed packet type (its `decode`, and the `ProtocolV1` registration that makes it
  reachable for a direction)
- a decoder or pipeline handler that reads peer bytes (`VarInt28FrameDecoder`,
  `PacketDecoder`, any new `ByteToMessageDecoder` / `ChannelInboundHandler`)
- a `ProtocolUtil` reader (VarInt, string, array) the packets share
- endpoint registration and dispatch: `registerIncoming` / `registerOutgoing`, unregister,
  routing an `Endpoint Message` to a user handler
- a state transition in the connection flow (handshake, `Available Endpoints` /
  `Supported Endpoints` and their acknowledgements, keep-alive, disconnect)

Then check each one against the guards below, and cite the line that applies each guard:

1. **The frame cap.** Peer input is bounded by `maximumFrameLength` before any buffer is
   allocated for it, and that cap never exceeds the 28-bit wire ceiling
   (`MAXIMUM_FRAME_LENGTH`). An encoder path that can emit what the peer's decoder refuses is
   this finding.
2. **Length-before-allocation.** Every length or count read from the wire is checked against
   `buffer.readableBytes()` (`ProtocolUtil.validateArrayLength`, `buffer.isReadable(length)`)
   before it sizes an array, a collection, a string, or a loop. A negative length is rejected.
   A `new T[length]` or `new ArrayList<>(length)` fed straight from a VarInt is the finding:
   name the frame size that triggers it and what it allocates. An `OutOfMemoryError` is an
   `Error`, so `catch (Exception e)` will not save the connection, or the process.
3. **Exact consumption.** A packet consumes exactly its frame's bytes; trailing bytes throw
   (`PacketDecoder`), except `Endpoint Message`, which is defined to take the remainder.
4. **Direction and registration.** An `Endpoint Message` is routed only for an endpoint that is
   available, supported, and whose flags permit this direction; endpoint ids match
   `namespace:key` (lowercase `a-z0-9_-`, at most 64 characters) and flags set at least one
   direction bit. A wrong-direction or unsupported message disconnects, or (server side only) is ignored with
   `Available Endpoints` resent, exactly as `protocol-v1.md` permits.
5. **Packet-order state.** A built-in packet that arrives in a state where the spec does not
   allow it (keep-alive before handshake, a second handshake, an acknowledgement nobody asked
   for) is a protocol error, not a no-op and not a state reset.
6. **Timeouts.** Every wait on the peer has the spec's bound: handshake, `Available Endpoints`
   acknowledgement, keep-alive response (and its echoed value checked), plus the client-side
   handshake-response and keep-alive-received waits. A new wait with no bound is the finding.

Where the entry point has a sibling on the other side of the wire (client decoder and server
decoder, `AvailableEndpointsPacket` and `SupportedEndpointsPacket`, client and server
disconnect conditions), list both guard sets side by side. A guard one side has and the other
lacks is a finding. The array pre-allocation bug lived on both directions at once; check both.
Where a guard has no analogue for this entry point, say so; do not manufacture a failure.

**Guard-set delta.** Then compare head against base (the diff is that comparison) and report
security posture that got weaker, even where the new code works as intended. Work per entry
point and per guarded operation the diff touches: list its guard set at base, list it at head,
and report every guard that fell out of the set. A removed guard is the finding even though the
removal is a deletion: tag `provenance` by where the guard lived, and say what the head now
accepts that the base refused. The shapes:

- validation weakened or removed: a length check, a cap, an id-format regex, a direction
  check, a disconnect condition turned into a log line or a silent drop
- a cap raised or made configurable past the wire ceiling, or a configurable cap whose default
  became "unbounded"
- a `ProtocolException` turned into something a catch-all swallows, or into an `Error`
- information leaking to the peer: stack traces, internal state, or another connection's data
  in a `Disconnect` reason or an error frame
- `ProtocolException.setDebugStackTraces(true)` reachable from a non-debug path (stack walks
  on every hostile throw are a CPU-burn vector)
- per-connection work the peer controls and nothing bounds: queued writes the peer never reads,
  pending acknowledgements, endpoint lists re-sent on every trigger
- a timeout removed or lengthened so a silent peer holds a connection (and its buffers) open

A deliberately removed check is not a weakened guard when it was unreachable: Bend deletes
dead defensive checks (`CLAUDE.md`, Testing / coverage). Confirm the unreachability from the
code before treating a deletion as safe, and file it when the case is in fact reachable.

State what the failure costs: an allocation sized by the peer is a one-packet remote
denial of service; a missing timeout is a connection slot held forever; a skipped direction
check hands the peer a handler it was never offered.
