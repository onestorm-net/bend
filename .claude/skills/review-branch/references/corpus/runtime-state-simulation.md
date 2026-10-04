# Runtime-state simulation

A clean static diff is not proof of behaviour. Execute the changed code (in your head, or by a
read-only probe) along the paths the happy-path test never takes. For each path below that the
diff can reach, say what happens; for each it cannot, say why not.

- **The second and Nth iteration**: state read after the first write; a second frame in the
  same buffer; a decoder's reader index after the first `decode()` call; a per-element
  allocation, registry lookup, or flush inside a loop over a peer-sized list.
- **The partial read**: bytes split across reads at every boundary that matters (inside the
  length VarInt, between length and body, inside a field). A decoder must leave the reader
  index unmoved and wait, never consume a prefix and hand back a shorter value. Walk the
  1-, 2-, 3- and 4-byte VarInt cases.
- **The cancelled or partial path**: the connection closes mid-handshake, mid-acknowledgement,
  mid-dispatch; a step that ran (an endpoint registered, a timer scheduled, a buffer retained)
  and is not undone when the channel goes inactive.
- **The connection that already exists**: an endpoint registered or unregistered while peers
  are connected obligates a fresh `Available Endpoints` / `Supported Endpoints` snapshot to
  each of them; a change that only affects new connections leaves live ones stale.
- **The non-default configuration**: a `maximumFrameLength` other than the one the tests use,
  a cap clamped by `Math.min` against `MAXIMUM_FRAME_LENGTH`, debug stack traces on.
- **The absent, empty, or zero value**: an empty endpoint list, a zero-length string, a frame
  length of `0` (canonical `0x00` is padding, `0x80 0x00` is not), flags `0`, a packet id the
  protocol never registered. Enumerate the adjacent unhandled case.
- **The non-canonical encoding**: a VarInt written with more bytes than its value needs. Bend
  decodes these tolerantly by ruling, so any guard keyed to the leading byte rather than the
  decoded value can be walked around.
- **The concurrent second actor**: the Netty event loop and a user thread at once (a user
  calling `register` / `unregister` / `send` while the event loop dispatches), or two event
  loops sharing one registry. Name the shared state, both actors, and the window between them,
  and what publishes the write across threads (a concurrent collection, `volatile`, execution
  on the channel's event loop), or say that nothing in this tree produces the second actor.
- **The in-flight message**: an `Endpoint Message` already decoded and queued for a handler
  whose endpoint is being unregistered. Say what the handler sees and what the peer sees.
- **The timer outliving its connection**: a scheduled timeout or keep-alive task still armed
  after the channel closed or the state it guards advanced. Walk each to its cancel site.
- **The multi-step flow's failure branch**: does a failure mid-flow send the `Disconnect` the
  spec requires, with the right `Type`? Does a retry or reconnect re-run a step that is not
  idempotent (a registration, a listener added twice)?
- **The element that throws**: one invalid `Endpoint` in an `Available Endpoints` list, one
  handler that throws during dispatch. First establish what the contract owes: reject the whole
  packet (protocol error) or isolate the element. Then simulate the throwing element against
  that contract: half the list applied, a handler exception that kills the connection for an
  unrelated endpoint, or one that vanishes.

A break on any of these is a real finding on a clean diff with a green build.
