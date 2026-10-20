# Security & Cost Surface: canonical questions

This is the **single source of truth** for the seven row questions that:

- the planner answers in PLAN.md's `## Security & Cost Surface` section during Phase 1.6 of `/plan-feature`
- the `surface-reviewer` agent grades the answers against at plan-time (prose-vs-Approach) and `precedent-reviewer` runs drift detection against at PR-time (prose-vs-code)

"Cost" here is resource cost: memory, CPU, threads and connections a peer can make the other side spend. Bend is a library speaking a custom wire protocol to peers it doesn't control, so the surface is the bytes that arrive, the connection they arrive on, and the API a library user calls.

The rows are deliberately **question-shaped, not field-shaped**. Filling in a form trains for the past; answering a question generalises to the next feature. Worked examples below each row show what a strong answer reads like; they are illustrative, not templates.

For the rationale and sycophancy guards, see [`quality-gates.md`](/.claude/skills/plan-feature/references/quality-gates.md) § Phase 1.6.

---

## Row 1: Untrusted wire input and allocation bounds

Every byte read off the channel was chosen by the peer. A malicious server can attack a client as easily as the reverse, so this row covers both directions.

Questions:

- Which new or changed fields does this feature read off the wire (a packet field, a length prefix, an array count, a string, an id, a flag set)? For each, which side sends it and which side decodes it?
- For every value that **sizes** something (an array, a `String`, a buffer, a collection, a loop bound), what check runs **before** the allocation or the loop, and against what bound? The array pre-allocation bug (`new Endpoint[length]` sized by a peer's VarInt before any readable check) is the shape: name the guard (`ProtocolUtil#validateArrayLength`, `buffer.isReadable(length)`, a declared maximum) and where it runs.
- Does every reader agree with its writer on width and limits? The frame-decoder bug was an encoder that could write a length the decoder refused to read back. If this feature adds or changes a reader or writer, name the counterpart and the shared constant or test that keeps them symmetric.
- Which exception does malformed input raise, and does it reach the pipeline as a `ProtocolException` (no stack trace by default, disconnects) rather than an `IllegalArgumentException`, an `IndexOutOfBoundsException`, or an `Error` that escapes the decoder's `catch (Exception e)`?
- Are the values validated against the spec's MUST rules on decode, not only on construction (character set and length of a namespaced id, at least one direction bit, trailing bytes after a packet's fields)?

*Strong answer:* "Field read: the `VarInt Array` of endpoint ids in `Supported Endpoints` (client → server, decoded by the server). `SupportedEndpointsPacket#decode` reads the count with `ProtocolUtil#readVarIntArray`, which calls `validateArrayLength` before `new int[length]`, so a count above `readableBytes()` throws `ProtocolException` before allocating. The writer is `ProtocolUtil#writeVarIntArray`, same file; both are covered by the existing round-trip tests. Ids are not range-checked on decode because an unknown id is a routing decision (Row 7), not a framing error. Proof: `SupportedEndpointsPacketTest#decode_arrayLengthExceedsReadable_throws`."

*N/A acceptable when:* the feature reads nothing new off the wire and changes no reader, writer or limit (e.g. a pure API rename in `client-api`).

---

## Row 2: Connection lifecycle, timeouts and silent peers

Questions:

- Which connection states does this feature add or change (before `Handshake`, awaiting `Available Endpoints`, awaiting an acknowledgement, established, closing)? Walk the happy path, then each failure: peer goes silent, peer half-closes, peer sends packets out of order, peer disconnects mid-flow, the local side closes first.
- For every state that waits on the peer, what bounds the wait? Name the timeout, its value, which `protocol-v1.md` rule sets it (the 30-second constants), and the mechanism that enforces it (an `IdleStateHandler`, a scheduled task on the channel's event loop). A wait with no timeout is how a slow-loris peer holds a connection forever.
- When the timeout or a violation fires, which side disconnects, does it send `Disconnect` first, and with which `Type` (`1: Protocol error`, `2: Unsupported version`, `3: Timeout`, `0: Generic`)?
- What does the local caller (the user of `client-api` or `server-netty`) observe when the connection dies in each state: a callback, a completed future, an exception, or nothing? "Nothing" is a silent failure; name the signal.
- Is anything scheduled per connection (a keep-alive task, a timeout) cancelled when the channel closes, so a closed connection leaves no task behind?

*Strong answer:* "New state: server awaits `Available Endpoints Acknowledged` after each resend. A `ScheduledFuture` on `ctx.executor()` fires after 30 s (protocol-v1.md § Disconnect conditions) and sends `Disconnect(3, "available endpoints not acknowledged")` then closes; the future is cancelled in `channelInactive` and when the matching revision is acknowledged. A stale acknowledgement (older revision) does not cancel it. The registry listener sees `onDisconnect(reason)` in every path. Proof: `ServerConnectionHandlerTest#availableEndpoints_noAcknowledgementWithin30s_disconnectsWithTimeout`."

*N/A acceptable when:* the feature adds no state, no wait on the peer, and no new disconnect path.

---

## Row 3: Threading and the event loop

Questions:

- Which thread runs each new piece of code: a Netty event loop, a caller's thread (a public API method), or an executor the feature owns?
- Does anything that can run on the event loop block: a lock held across I/O, a `sync()`/`await()` on a future, a user-supplied handler doing arbitrary work, a blocking queue? Name where user callbacks (endpoint handlers, listeners) execute and whether a slow handler stalls every other connection on that loop.
- Which state is shared across threads (the endpoint registry, a connection map, a revision counter), and what makes it safe: confinement to one event loop, a concurrent collection, `volatile`, or a lock? Name it.
- For public API methods callable from any thread (`send`, `register`, `unregister`, `close`), how does the call hand off to the channel's event loop, and is ordering preserved between two calls from the same thread?

*Strong answer:* "Endpoint handlers run on the channel's event loop (D3: no separate executor in 0.1; documented in the `EndpointHandler` Javadoc as 'must not block'). The registry is a `ConcurrentHashMap` because `register` is called from user threads; dispatch reads it on the event loop. `Client#send` from a user thread goes through `channel.writeAndFlush`, which Netty marshals onto the event loop in call order (verify at implementation: `AbstractChannelHandlerContext#write` from a non-loop thread). Proof: `NettyClientTest#send_fromTwoThreads_preservesPerThreadOrder`."

*N/A acceptable when:* the feature adds no code that runs on the event loop, shares no state across threads, and adds no public method callable from another thread.

---

## Row 4: Resource ownership

Questions:

- For every `ByteBuf` the feature creates, receives or passes on: who owns it, and who releases it on the success path **and** on every failure path (decode throws, handler throws, write fails, channel already closed)? Name the `release()`, the `try/finally`, or the Netty base class that releases for you (`SimpleChannelInboundHandler`, `MessageToByteEncoder`; verify at implementation which ones do).
- Where a `ByteBuf` crosses into the public API it becomes a `java.nio.ByteBuffer`: is that a copy, or a view whose backing buffer gets released while the user still holds it?
- What per-connection memory or state can the peer make grow (queued outbound messages, pending acknowledgements, a buffer accumulating a partial frame, registered handlers), and what bounds each one? `maximumFrameLength` bounds one frame; name the bound for everything else.
- For every `ChannelFuture` the feature produces (`writeAndFlush`, `close`, `connect`), is there a listener that observes failure? An unobserved failed write is a silent drop.
- When the connection closes, what is freed: buffers, scheduled tasks, registry entries, user-facing handles?

*Strong answer:* "`EndpointMessagePacket` payloads are copied into a heap `ByteBuffer` (`ByteBuffer.wrap(ByteBufUtil.getBytes(buf))`) before the user handler sees them, so the frame buffer is released in `PacketDecoder`'s existing `finally` regardless of handler behaviour. Outbound: `send` wraps the user's `ByteBuffer` with `Unpooled.wrappedBuffer` (no copy; the user must not mutate it until the returned `CompletableFuture` completes, stated in the Javadoc). Every `writeAndFlush` gets a listener that completes that future exceptionally on failure. Per-connection growth: none new; outbound backpressure stays Netty's default high-water mark (verify at implementation). Proof: `EndpointDispatchTest#dispatch_handlerThrows_releasesFrame` with `ResourceLeakDetector` at `PARANOID`."

*N/A acceptable when:* the feature allocates no buffer, adds no per-connection state, and produces no `ChannelFuture`.

---

## Row 5: Spec conformance

`protocol/protocol-v1.md` is the contract a from-spec implementer in another language builds against. The implementation and the spec drifting apart is a bug in whichever one is wrong.

Questions:

- Which MUST, MUST NOT, SHOULD and disconnect rules in `protocol-v1.md` does this feature implement or touch? Quote them by section.
- For each rule: is it enforced on **both** sides that the spec binds (the server-side Disconnect conditions and the Client-side disconnect conditions are separate lists), and where in the code?
- Does the feature change the wire format or a rule? Then name the exact `protocol-v1.md` edit (section and new text), whether the change is backwards-compatible with a peer on the current spec, and whether it needs a protocol version bump in `Handshake`.
- Does the plan add a check that the current code cannot reach? Bend's rule (CLAUDE.md § Testing / coverage, ADR 0009): an unreachable "defensive" check is deleted, not kept. Show the input that reaches it, or drop it.
- Conversely, does the plan rely on a property the spec leaves open (canonical VarInt encoding, which side sends first after an update) without writing it down?

*Strong answer:* "Implements protocol-v1.md § Endpoint list updates: 'Before sending the updated list, the sender MUST stop sending `Endpoint Message`s on any removed endpoints.' Server side: `EndpointRegistry#unregister` removes the id from the outbound send set before `AvailableEndpointsPacket` is written (same event-loop task). Client side is out of scope (BD-0015). No wire change, no spec edit, no version bump. Proof: `ServerEndpointRegistryTest#unregister_thenSend_rejectsBeforeListIsWritten`."

*N/A acceptable when:* the feature touches no behaviour `protocol-v1.md` describes (e.g. a build or test-only change).

---

## Row 6: Public API surface, module boundaries and enforcement

Questions:

- Which public types and methods does this feature add to `client-api` (or to `server-netty`'s future-public register/unregister/handler surface)? For each: does it expose any Netty type (`ByteBuf`, `Channel`, `ChannelFuture`, `EventLoop`)? CLAUDE.md § API design decisions bans this; payloads cross as `java.nio.ByteBuffer`.
- Does any module gain a dependency? `client-api` depends on nothing; implementation modules may depend on their api module and `protocol`. Name the `build.gradle.kts` change, or confirm there is none.
- Does the API keep the two send paths separate: built-in packets written internally, user payloads through the public, registration-checked path?
- What does a caller get wrong most easily with this API (calling before connect, after close, registering twice, mutating a buffer after `send`), and is that an `IllegalStateException` / `IllegalArgumentException` at the call, or silent misbehaviour?
- Every new public type in `client-api` (and `server-api`, once it exists) needs Javadoc; name the types.
- For each new structural rule the plan introduces ("every packet validates on decode", "no handler blocks", "every array reader calls `validateArrayLength`"), what enforces it? Bend has no linter or architecture tests, so the ladder is:
  - **L1**: compile time (module dependency absent from `build.gradle.kts` so the import cannot compile, a `final` or sealed type, a method signature that cannot accept the wrong thing)
  - **L2**: a test that fails when the rule is broken (name it)
  - **L3**: runtime check at construction or first use
  - **L4**: review only, acceptable only when chosen explicitly with the trade-off stated

*Strong answer:* "Adds `EndpointHandler` (`void handle(ByteBuffer payload)`) and `Client#register(String id, EndpointHandler)` to `client-api`; no Netty type in either signature, enforced at L1 because `client-api/build.gradle.kts` has no Netty dependency, so the import cannot compile. Registering an id twice throws `IllegalStateException`. Javadoc on `EndpointHandler`, `Client#register`, `Client#unregister`. New rule 'handlers must not block': L4, stated in Javadoc, trade-off recorded in D3 (an executor hop costs a copy and an ordering guarantee). Proof: `NettyClientTest#register_sameIdTwice_throws`."

*N/A acceptable when:* the feature adds no public type or method, no module dependency, and no new structural rule.

---

## Row 7: Runtime endpoint add and remove

Adding and removing endpoints on a live connection is a core protocol feature, and the future module system (hot-swapping a module jar on a running server) depends on it being correct under traffic.

Questions:

- Can this feature register or unregister an endpoint while connections are open? If so, walk the update cycle in protocol-v1.md § Endpoint list updates for both an add and a remove: when the new list is sent, when new endpoints become usable (not before the acknowledge cycle completes), when removed ones stop being sent and stop being accepted.
- What happens to an `Endpoint Message` that is **already in flight** when its endpoint is removed: decoded but not yet dispatched, dispatched to a handler that is mid-execution, or queued outbound? Name what the handler, the sender and the peer each see.
- What happens when two list updates overlap (a second `Available Endpoints` sent before the first is acknowledged)? Which revision wins, and what keeps an old acknowledgement from enabling the new list?
- Does an incoming message on a removed, unknown or wrong-direction endpoint follow the spec's rule (disconnect, or for the server optionally ignore and resend `Available Endpoints`), and which one does this implementation pick?
- Is the client's supported set recomputed (available ∩ registered) and resent as a whole snapshot on both triggers (new available list received; new local registration that is available)?

*Strong answer:* "Unregister on the server: the id is removed from the dispatch table first, so a message decoded after that point is treated as 'unsupported endpoint' and dropped with an `Available Endpoints` resend (spec MAY, D5). A handler already executing finishes; no interruption. Outbound messages queued before the unregister are flushed; none can be queued after it because `send` checks the table on the event loop. Overlapping updates: acknowledgements carry the revision; only the latest revision's acknowledgement enables new ids. Proof: `ServerEndpointRegistryTest#unregister_messageDecodedAfterRemoval_isDroppedAndListResent`."

*N/A acceptable when:* the feature cannot change the endpoint set of a live connection and does not touch endpoint routing.

---

## Proof: every row ends with the test that would catch it

Each row's prose ends with a **Proof** line: the test class and method (`ClassNameTest#method_condition_expected`) that fails if the row's answer is wrong, or `Proof: N/A: <reason>` when the row itself is N/A. "Covered by the existing suite" is THIN; name the case. A test that replays the answer cannot fail: a threading claim "proved" on an `EmbeddedChannel` (which runs everything on the calling thread) proves nothing about the event loop, a timeout test that never advances a clock or scheduler cannot see the timeout, and a leak claim checked without `ResourceLeakDetector` reporting cannot see a leak. A test that exists but cannot fail is the largest single theme in review findings, ahead of every defect it was written to catch. The Proof line is written at plan time and checked by `precedent-reviewer` at PR time against the diff, the same way the row prose is.

---

## How the planner uses this file (Phase 1.6)

Read each row, write a prose paragraph in PLAN.md's `## Security & Cost Surface` section answering the row's questions for *your* feature, and end it with the Proof line. Mark `N/A: <one-line reason>` if no question on the row applies, but check the N/A clause first: anything that reads from the wire or touches a live connection rarely qualifies.

Paraphrasing the questions back instead of answering them is THIN, not OK (Phase 4d rule). The `surface-reviewer` agent flags THIN prose at Phase 5.

## How `surface-reviewer` uses this file

Loads this file once at Step 1. For each row, compares the planner's prose against what the Approach section says the implementation will do.

At PR-time, `precedent-reviewer` holds the shipped code against the Surface prose itself. A row that passed at plan-time and fails against the diff is a contradicted claim in that prose; the reviewer weighs it by what the divergence costs, with a contradicted safety or resource claim as the blocker case. No plan-time verdict is persisted for it to read: the prose is the record.
