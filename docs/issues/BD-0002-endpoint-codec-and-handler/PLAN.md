# BD-0002-endpoint-codec-and-handler: EndpointCodec and EndpointHandler on java.nio.ByteBuffer

**Date:** 2026-10-25
**Status:** Approved
**Issue:** [BD-0002](https://onestorm.kendo.dev/projects/1/issues/BD-0002)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0002-endpoint-codec-and-handler/DECISIONS.md)

## Goal
A client author can define how their payload type turns into bytes and back, and receive decoded
messages, against a written contract that uses only `java.nio.ByteBuffer`, so their code never
depends on Netty.

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| Codec shape | `public interface EndpointCodec<T>` with `T decode(ByteBuffer payload)` and `ByteBuffer encode(T message)`; no `getType()` | Issue AC 1; the pre-migration `EndpointCodec` minus `getType()` and `ByteBuf` (ADR 0012 Context names this rework) |
| Handler shape | `@FunctionalInterface public interface EndpointHandler<T>` with `void handle(T message)`; no `Client` parameter | Issue AC 4; `Packet`-style extension point exempt per the module-shape lens |
| Payload type | `java.nio.ByteBuffer` in both directions | CLAUDE.md § API design decisions ([0012](/docs/decisions/0012-no-netty-types-in-api.md)) |
| Encode ownership | Returned buffer is handed over; codec must not change it afterwards; read from position to limit (D1) | ADR 0012 "Does not decide": copy vs view is each feature's call |
| Decode buffer | Read-only view, position to limit, big-endian, valid only during the call; nothing referring to it may be kept (D2) | `EndpointMessagePacket#decode` (`readRetainedSlice`): the payload is a slice of the frame |
| Null and leftovers | Neither method returns `null` (a `null` counts as a failed encode/decode); unread bytes are ignored (D3) | Issue AC 5 (failure semantics) |
| Failure semantics | Failed encode: the send fails, nothing is written. Failed decode or throwing handler: that message is lost, the implementation reports it, the connection stays open (D4) | Issue AC 5; protocol-v1.md § Client-side disconnect conditions lists no payload-level condition |
| Package | `net.onestorm.bend.client.api`, next to `NamespacedId`; layout reviewed before 0.1 (D5) | `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java` |
| Threading | Not stated here; BD-0006 adds it (D6) | Kendo BD-0006 |
| Javadoc | Class Javadoc carrying the contract, `@param <T>`, and method Javadoc with `@param`/`@return`; no `@throws` tag, because the contract names no exception a codec should throw: what any throw does is stated in the method text (D4) | `client-api/CLAUDE.md` (ADR 0021); `NamespacedId.java` style |
| No Lombok | Plain interfaces | `client-api/CLAUDE.md` (BD-0001 D8) |

**Layer compliance:** both interfaces live in `client-api` and import only `java.nio.ByteBuffer`.
`client-api/build.gradle.kts` declares only `testImplementation(libs.assertj.core)`, and
CLAUDE.md § API design decisions requires no main dependencies, so this fits.

**Module shape:**

| Class | Inputs/outputs | Hidden behaviour | Test seam | Shallow-test reckoning | Verdict |
|---|---|---|---|---|---|
| `EndpointCodec<T>` | `decode(ByteBuffer)` → `T`; `encode(T)` → `ByteBuffer` | None in `client-api`: the user implements it; the contract (ownership, validity, failure) is the content | `client-netty` round-trip and failure tests against the contract | A second use case (another payload type) is another implementation, not a method. A different shape (streaming, size hints) would be a new method or type, which is why none is added before a caller exists | Shallow-but-justified (extension point, exempt per the lens) |
| `EndpointHandler<T>` | `handle(T)` | None; user callback | `client-netty` dispatch tests | A second use case is another lambda. Context such as the endpoint or client would be a parameter change, rejected in the issue (replies go through an outgoing handle, BD-0003) | Shallow-but-justified (callback, exempt per the lens) |

## Scope
- **In scope:**
  - `client-api/src/main/java/net/onestorm/bend/client/api/EndpointCodec.java` (new)
  - `client-api/src/main/java/net/onestorm/bend/client/api/EndpointHandler.java` (new)
  - `client-api/src/main/java/net/onestorm/bend/client/api/package-info.java`: name the codec and handler next to `NamespacedId`
- **Out of scope:**
  - VarInt/String helpers for codec authors (issue Scope)
  - `getType()` and any lookup by payload class (replaced by BD-0003's typed handles)
  - Threading and blocking rules (BD-0006, D6)
  - An error-listener API for codec and handler failures (D4)
  - `send`, its future and `EndpointUnavailableException` (BD-0004); registration (BD-0003)
  - A test class in `client-api` (D7)
  - Any change to `protocol` or `protocol/protocol-v1.md`
  - Reorganising `client-api` packages (D5; a separate pre-0.1 issue)

## Approach

1. **`EndpointCodec.java`**: `public interface EndpointCodec<T>`. Class Javadoc:
   - what it is: converts one endpoint's messages of type `T` to and from the bytes of an `Endpoint Message` payload; one codec per registered endpoint;
   - `@param <T>` the message type.
   `decode(ByteBuffer payload)` Javadoc: the payload is the bytes between `payload`'s position and limit, chosen by the peer, so a length the codec reads from it must be checked before it sizes an allocation; the buffer is read-only and starts in big-endian order; it is valid only during this call, so neither the codec nor the returned value may keep it or anything that refers to it (a slice, duplicate or view); bytes left unread are ignored; must not return `null`; if it throws (or returns `null`) the message is lost, the implementation reports the failure, and the connection stays open (D2 to D4). `@param payload`, `@return the decoded message, never {@code null}`.
   `encode(T message)` Javadoc: returns a buffer whose bytes from position to limit are sent as the payload; the buffer is handed over and the codec must not change it after returning; any kind of buffer may be returned and an empty one sends an empty payload; must not return `null`; if it throws (or returns `null`) the send fails and nothing is written (D1, D3). `@param message`, `@return`.
2. **`EndpointHandler.java`**: `@FunctionalInterface public interface EndpointHandler<T>`. Class Javadoc: receives the decoded messages of one incoming endpoint; `@param <T>`. `handle(T message)` Javadoc: called once per message, with the value the endpoint's codec decoded (never `null`); if it throws, that message is lost, the implementation reports the failure, and the connection stays open (D4). `@param message`.
3. **`package-info.java`**: the second sentence names `{@link EndpointCodec}` and `{@link EndpointHandler}` as the interfaces a user implements, alongside the value type `NamespacedId`.
4. Run `./gradlew :bend-client-api:javadoc`, `./gradlew :bend-client-api:dependencies --configuration compileClasspath` and `./gradlew build`.

## Security & Cost Surface

### Untrusted wire input and allocation bounds
This issue adds no reader or writer and changes no limit. The bytes it concerns are the `Message
payload` of an `Endpoint Message` (server to client, decoded by `client-netty`, and client to
server, encoded by the user's codec). Inside Bend the payload stays opaque: its size is bounded by
the frame cap before any codec sees it, and `client-netty` hands the codec a view of bytes already
in memory (D2), so Bend allocates nothing sized by the payload's contents. The codec is the user's
own parser of untrusted server bytes. Whatever it allocates from a length it reads is the codec's
responsibility, and a codec that throws on malformed input loses only that message (D4), never the
connection, so a hostile payload cannot turn a user parsing bug into a disconnect loop. A codec
exception never surfaces as a `ProtocolException`: it is an application error, not a framing
error. Proof: N/A in `client-api` (interfaces only, D7); `client-netty`'s dispatch tests feed a
throwing codec when the client-netty epic builds dispatch.

### Connection lifecycle, timeouts and silent peers
N/A: no state, wait or disconnect path. D4 explicitly adds no disconnect for codec or handler failures.

### Threading and the event loop
N/A in this issue: two interfaces with no code. Which thread runs `decode`, `encode` and `handle`,
and whether they may block, is BD-0006's contract (D6). The one property this issue fixes that
crosses threads is D1: because the encoded buffer is handed over, the write that happens later on
the I/O thread reads a buffer the codec no longer touches.

### Resource ownership
This issue settles copy versus view at the `ByteBuffer` boundary (ADR 0012 leaves it to each
feature). Inbound: `client-netty` passes a read-only view over the payload slice that
`EndpointMessagePacket#decode` retains (`readRetainedSlice`). The view is valid only during
`decode`, so `client-netty` can release the slice as soon as `decode` returns or throws; the
contract forbids the codec and its result from keeping the buffer or a view of it (D2). Read-only
means a codec cannot corrupt the frame. Outbound: the buffer `encode` returns is handed over (D1),
so `client-netty` can wrap it without copying and write it later; the codec must not change it.
A codec that breaks either rule sees undefined data, which is the trade-off accepted for zero
copies in both directions. Per-connection growth: none added. Proof: N/A in `client-api`; the
release-on-throw path is `client-netty`'s, to be proved there with `ResourceLeakDetector` at
`PARANOID` when dispatch is built.

### Spec conformance
Touches protocol-v1.md § Endpoint Message: "Message payload | Byte Array | The payload, length is
the remaining bytes in the frame". The contract matches it: the codec sees exactly those bytes
(position to limit), and an empty payload is valid in both directions (the frame still holds the
endpoint id VarInt, so it is not a zero-length frame under ADR 0003). § Client-side disconnect
conditions lists no payload-level condition, so D4's "connection stays open" adds no rule the spec
contradicts. No spec edit, no version change, no check added. Proof: N/A: no code; the
empty-payload round trip belongs to `client-netty`'s tests.

### Public API surface, module boundaries and enforcement
New public types: `net.onestorm.bend.client.api.EndpointCodec<T>` (`decode`, `encode`) and
`EndpointHandler<T>` (`handle`), both with Javadoc. No Netty type appears in either signature,
enforced at L1: `client-api/build.gradle.kts` has no main dependency, so an `io.netty` import
cannot compile. No module gains a dependency. Neither type touches the send paths (ADR 0008):
the codec only produces the payload that BD-0004's public send wraps in `Endpoint Message`. The
rules a user can break (keeping the decode buffer, changing a returned buffer, returning `null`)
are enforced at L4, in Javadoc only, chosen deliberately: checking them would cost a copy per
message (D1, D2), and a `null` result is caught at L3 by the implementation treating it as a
failure (D3). Proof: `./gradlew :bend-client-api:dependencies --configuration compileClasspath`
prints "No dependencies", and `./gradlew :bend-client-api:javadoc` prints no "no comment" warning.

### Runtime endpoint add and remove
N/A: no registration or routing; BD-0003 registers codecs and handlers on endpoints.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | `EndpointCodec<T>` declares exactly `T decode(ByteBuffer)` and `ByteBuffer encode(T)`; `EndpointHandler<T>` is a `@FunctionalInterface` with exactly `void handle(T)` | Read both sources; `./gradlew :bend-client-api:compileJava` |
| 2 | The decode Javadoc states: read-only, position to limit, big-endian, valid only during the call, no reference or view kept, unread bytes ignored, non-null result, failure loses the message, is reported, connection stays open | Read `EndpointCodec.java` |
| 3 | The encode Javadoc states: position to limit is sent, the buffer is handed over and must not change afterwards, any kind of buffer is accepted, an empty buffer is an empty payload, non-null result, a failure fails the send and writes nothing | Read `EndpointCodec.java` |
| 4 | The handler Javadoc states: called once per decoded message, never with `null`, and a throwing handler loses that message, is reported, and leaves the connection open | Read `EndpointHandler.java` |
| 5 | Every public type and member, including type parameters, has Javadoc | `./gradlew :bend-client-api:javadoc` prints no `warning: no comment` and no `no @param for <T>` line (Gradle's own restricted-method warnings are expected and unrelated) |
| 6 | `client-api` still has no main dependency | `./gradlew :bend-client-api:dependencies --configuration compileClasspath` prints "No dependencies" |
| 7 | `package-info.java` names `EndpointCodec` and `EndpointHandler` next to `NamespacedId`, and the links resolve | Read the file; `./gradlew :bend-client-api:javadoc` reports no broken `{@link}`; `./gradlew build` passes |

## Reuse and patterns

- `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`: mirror: Javadoc voice, `{@code}` use, `@throws`/`@return` shape, package
- `client-api/src/main/java/net/onestorm/bend/client/api/package-info.java`: mirror: edit in place, same sentence style
- `protocol/src/main/java/net/onestorm/bend/protocol/packet/EndpointMessagePacket.java`: mirror (read only): where the decode payload comes from, so the "valid only during the call" wording matches what `client-netty` can deliver
- The pre-migration `EndpointCodec`/`EndpointHandler` (see `.claude/local-references.md`): mirror the method set only; drop `getType()`, `ByteBuf` and the `Client` parameter

## Testing Strategy

No test class in this PR (D7): both types are interfaces without default methods, so there is no
code to execute, and a lambda-assignment test cannot fail for any change to the contract. The
promised behaviour is tested where it is implemented, in `client-netty`: a round trip through a
codec, an empty payload, a `decode` that throws, a `decode` returning `null`, a handler that
throws, and the payload slice released on each of those paths. Those tests are planned with the
client-netty epic, not here.

Verification for this PR: `./gradlew :bend-client-api:javadoc` (AC 5; JDK 25's javadoc reports
`warning: no comment` for an undocumented public member by default, confirmed during BD-0001 and
recorded in Kendo BD-0010, so this check can fail; re-check once by deleting one comment) and
`./gradlew :bend-client-api:dependencies --configuration compileClasspath` (AC 6), then
`./gradlew build`.

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: the payload format is unchanged and stays opaque |
| New or changed public type or method in `client-api` | Javadoc on that type | Update: `EndpointCodec`, `EndpointHandler`, `package-info.java` |
| A design decision, layer rule, convention or module plan | `CLAUDE.md` / ADR | N/A: D1 to D7 are this issue's contract, recorded in its DECISIONS.md and carried by the Javadoc; no project-wide rule changes. The package layout review (D5) is a separate Kendo issue |

## Edge Cases
- Empty payload: `decode` receives a buffer with nothing remaining; `encode` may return an empty buffer. Both are valid (spec § Endpoint Message; the frame holds the endpoint id).
- `decode` or `encode` returns `null`: treated as a failed decode or encode (D3).
- A codec keeps a slice or duplicate of the decode buffer: contract violation, stated in the Javadoc (D2); its contents are undefined after the call.
- A codec changes a buffer after returning it from `encode`: contract violation (D1); what is sent is undefined.
- `decode` reads only part of the payload: the rest is ignored (D3).
- A handler throws: that message is lost, the failure is reported, the connection stays open (D4).

## Risks
- **Zero-copy contracts break silently.** A codec that reuses one buffer for every `encode`, or returns a view of the decode buffer, corrupts data without an error. The Javadoc must say this plainly with the reason; there is no cheap runtime check (D1, D2; Security & Cost Surface, public API row).
- **"Reported" needs a mechanism.** D4 promises the implementation reports a failed decode or a throwing handler, but `client-netty` has no logging dependency or convention yet. The client-netty epic has to choose one to keep this Javadoc's promise.
- **Wording ahead of BD-0003/BD-0004.** The Javadoc describes "the send" and "the endpoint" before `OutgoingEndpoint#send` exists, so it can't `{@link}` them. BD-0003 and BD-0004 must keep these semantics (a failed encode fails the send's future) and add the links.
- **Handover across threads.** D1 assumes the codec's writes to the returned buffer are visible to the I/O thread that later writes it. That relies on Netty's task submission to the event loop giving a happens-before edge; verify at implementation in `client-netty`.
- **Byte order.** The "starts in big-endian order" promise is `client-netty`'s to keep: check at implementation that its view (e.g. `nioBuffer().asReadOnlyBuffer()`) is big-endian, or set it explicitly.
