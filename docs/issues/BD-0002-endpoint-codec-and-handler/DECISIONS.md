# BD-0002-endpoint-codec-and-handler: Decisions

## D1: The buffer `encode` returns is handed over to Bend
**Status:** Accepted

**Context:** ADR 0012 leaves copy versus view at the `ByteBuffer` boundary to each feature; the implementation writes the payload after `encode` returns, possibly on another thread.

**Decision:** The Javadoc says the returned buffer belongs to Bend from then on and the codec must not change it, so `client-netty` can wrap it without a copy. Bend reads from its position to its limit; any buffer kind (heap, direct, read-only) is accepted.

**Rejected:**
- Copying on return so a codec can reuse one scratch buffer: every send pays a copy, for a pattern few codecs use.

## D2: `decode` gets a read-only view, valid only during the call
**Status:** Accepted

**Context:** The payload arrives as a slice of the frame's `ByteBuf` (`EndpointMessagePacket#decode`, `readRetainedSlice`), which is released after dispatch.

**Decision:** `decode` receives a read-only view of the payload, from position to limit, starting in big-endian order. The codec must not keep the buffer or anything that refers to it (a slice, a duplicate, a view) past the call; the decoded value has to own its data.

**Rejected:**
- A copied heap buffer or `byte[]`: a copy on every message, because pooled Netty buffers usually aren't backed by an array (the issue's reason).

## D3: `decode` returns non-null; unread bytes are ignored
**Status:** Accepted

**Context:** The contract has to say what a `null` result or a partly read payload means.

**Decision:** `decode` must not return `null`, and `encode` must not return `null`; a `null` result counts as a failed decode or encode. Bytes `decode` leaves unread are ignored, because the payload format belongs to the codec.

**Rejected:**
- Treating leftover bytes as a failed decode: pushes one format rule onto every codec.
- Passing a `null` decode result to the handler: every handler would have to handle it.

## D4: A throwing handler counts as a failed decode; the implementation reports it
**Status:** Accepted

**Context:** The issue says a failed decode "drops that one message and is reported" but not how, and says nothing about a handler that throws.

**Decision:** A handler that throws is treated like a failed decode: that one message is lost and the connection stays open. The Javadoc says the implementation reports the failure (`client-netty` logs it); `client-api` gets no error-listener type in this issue.

**Rejected:**
- An error callback in `client-api` now: larger scope, and it overlaps BD-0005's connection events.
- Disconnecting on a handler exception: it is an application error, not a protocol violation.

## D5: Both interfaces go in `net.onestorm.bend.client.api` for now
**Status:** Accepted

**Context:** The pre-migration code had a `codec` subpackage; `NamespacedId` sits in the flat package.

**Decision:** `EndpointCodec` and `EndpointHandler` go in `net.onestorm.bend.client.api`, next to `NamespacedId`. The package layout of `client-api` as a whole is reviewed before the 0.1 release (BD-0013).

**Rejected:**
- `net.onestorm.bend.client.api.codec` now: splits a small API before the rest of its types (BD-0003 to BD-0005) exist.

## D6: Threading stays out of this issue's Javadoc
**Status:** Accepted

**Context:** Which thread runs `decode` and `handle`, and whether they may block, is BD-0006's threading contract.

**Decision:** The Javadoc of both interfaces says nothing about threads; BD-0006 adds it to these and the other interfaces together.

**Rejected:**
- Stating "runs on the I/O thread, must not block" now: decides BD-0006's question in a narrower issue.

## D7: No test class in `client-api`
**Status:** Accepted

**Context:** Both types are interfaces without default methods, so they compile to no executable code.

**Decision:** No test class is added; a lambda-assignment test would only test the compiler (ADR 0009's reasoning against contrived tests). Verification is `./gradlew :bend-client-api:javadoc` and the empty dependency block; the behaviour the Javadoc promises is tested in `client-netty`.

**Rejected:**
- A test that implements both interfaces and calls them: it cannot fail for any change to the contract.
