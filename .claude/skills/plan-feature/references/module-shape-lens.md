# Module-shape lens (deep vs shallow)

A **deep module** hides a lot of behaviour behind a small interface and rarely changes: easy to test in isolation, hard to break. A **shallow module** is a thin wrapper whose interface is almost as complex as its implementation, usually a sign that the abstraction isn't carrying its weight.

"Module" here means a class or interface the feature introduces, not a Gradle module.

> **The most common author failure mode**: rationalising "Deep" because the implementation has lots of code inside. **Implementation line count is NOT the test.** Authors get a handler with 200 lines of state-machine code inside, see "lots of behaviour," and stamp "Deep." Wrong test. The right test is interface-vs-implementation *ratio* per use case; see the shallow-detection test below.

## The shallow-detection test (the load-bearing part)

A class is **shallow-and-suspect** if any of these three checks fires:

1. **The interface mirrors the use case.** One public method whose name *is* the use case (`sendHandshakeAndAwaitEndpoints`, `registerChatEndpoint`, `disconnectForTimeout`), whose parameters describe its whole job in primitives. The method name + signature could be lifted into a comment on the caller and nothing important would be lost.
2. **The interface is roughly proportional to the implementation per use case.** 5 primitive params + 1 method ⇒ interface complexity ≈ implementation complexity, even when the method body is 150+ lines. The body's size is irrelevant; what matters is whether the *interface* shrinks the caller's burden.
3. **A second consumer would require adding a method, not a parameter.** Concrete forcing question: "what about <plausible next use case> tomorrow?" If the honest answer is "add `disconnectForProtocolError`," the class is per-use-case scaffolding, not a protocol-level abstraction. Each new use case will duplicate the lifecycle code.

If a class fails any of these, it's shallow-and-suspect. Two ways to fix:

- **Demote it.** Push the use-case method down into the caller; the class disappears. The caller gets deeper (now owns orchestration). Whatever protocol or lifecycle logic was hiding inside gets exposed as its own deep low-level primitive.
- **Promote it.** Replace the per-use-case methods with protocol primitives the caller composes. Now the class is reusable across use cases and the interface no longer mirrors any single one.

## Calibration: Bend's own deep modules

The existing `protocol` code shows both shapes, and the deep ones are the precedent to mirror:

- `ProtocolUtil` is **deep**: a handful of primitives (`readVarInt`, `writeVarInt`, `readString`, `readVarIntArray`, `validateArrayLength`) that every packet composes. A new packet with a new field shape adds at most one primitive, never a per-packet method. The array pre-allocation fix landed as one new primitive (`validateArrayLength`) called from both readers, which is the deep shape doing its job.
- `VarInt28FrameDecoder` is **deep**: one Netty-dictated entry point (`decode`) and one constructor parameter (`maximumFrameLength`) hide NUL-padding skipping, partial-read handling, the 28-bit reader and the length policy. A new framing policy is a parameter, not a method.
- `Disconnect` handling is the shape to watch: a `disconnectForTimeout(ctx)`, `disconnectForProtocolError(ctx)`, `disconnectForVersion(ctx)` trio fails test #3 (each new reason is a method). `disconnect(ctx, type, reason)` with the spec's `Type` values is the deep version.

Plans for new connection handlers and registries (`server-netty`'s endpoint registry, the client's connection state) should mirror this shape from day 1: a small set of primitives callers compose, not one method per flow step.

## In scope

Apply the lens to: pipeline handlers that carry Bend logic, endpoint registries and dispatchers, connection-state classes, public `client-api` interfaces with behaviour behind them, utility classes, and any class introduced specifically for this feature whose shape isn't dictated by a framework or language convention.

## Exempt: shape dictated by the role

Skip the lens for these; their shallowness is intrinsic to the role, not a design smell:

- Packet classes (`KeepAlivePacket`-style Lombok classes implementing `Packet`: fields plus `decode`/`encode`)
- Records and Lombok value classes (`@Getter`/`@Value`/`@AllArgsConstructor` carriers with no behaviour)
- Exception classes (`ProtocolException`-style: a message, maybe a cause)
- Netty `ChannelInitializer` subclasses (`initChannel` adds handlers to the pipeline, nothing else)
- Simple interfaces with one obvious method that exist as an extension point (`Packet`, an `EndpointHandler` callback)
- Builder classes that only collect settings and construct the implementation

The distinction that matters: a packet class is exempt even though it is shallow. A new *handler* or *registry* whose entire interface is one method per protocol step is **not** exempt; it is the canonical shallow module the lens catches.

## What to answer per in-scope class

For each row in PLAN.md's Key Design Decisions / module-shape table:

- **Inputs/outputs**: the smallest interface that does the job
- **Hidden behaviour**: what this class knows that callers don't have to
- **Test seam**: what you'd test through the public interface, in plain English ("can register, then route, then unregister while a message is in flight")
- **Shallow-test reckoning**: explicitly answer: "if a second use case lands, do I add a method or a parameter?" Method ⇒ shallow. Parameter ⇒ likely deep. This is the bit that catches author rationalisation.
- **Verdict**: deep, shallow-but-justified (e.g. a one-shot adapter, or a precedent-cited pattern with explicit rationale), or shallow-and-suspect

A verdict of "Deep" without the shallow-test reckoning is **not** an acceptable lens output. Authors must show the test, not just label the result.

## Resolution

If a class comes back **shallow-and-suspect**, demote or promote it before continuing. Don't ship a five-shallow-classes-glued-together design. Plan-time is the last cheap moment: no pre-PR reviewer grades module depth directly, so a shallow design surfaces later only as the sibling drift and dead scaffolding `precedent-reviewer` flags, after it's built.

## Output

Goes into PLAN.md's "Key Design Decisions" table (or a dedicated module-shape sub-table) as one row per non-trivial class, with the test seam captured separately under "Testing Strategy". Every row must include the **Shallow-test reckoning** column, not just a verdict label.
