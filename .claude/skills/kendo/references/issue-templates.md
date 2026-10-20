# Issue Templates

Single source of truth for how Bend issues are written. Used by `/newbranch`, `/plan-feature`,
`/task-writer`, and any skill that creates issues through `mcp__kendo__create-issue-tool`.

Bend is a library, so the "user" in an issue is whoever consumes it: an **API consumer** writing
code against `client-api` (or a future `server-api`), or a **protocol implementer** writing a
peer from `protocol/protocol-v1.md`. Acceptance criteria describe what they can observe: public
API behaviour, bytes on the wire, when a connection is closed and with which `Disconnect` type.

## Contents

1. [Issue Types & Priorities](#issue-types--priorities)
2. [Feature Template (User Story)](#feature-template-user-story)
3. [Bug Template (Bug Report)](#bug-template-bug-report)
4. [Task](#task)
5. [What done looks like](#what-done-looks-like)
6. [Writing Principles](#writing-principles)
7. [Examples](#examples)

---

## Issue Types & Priorities

These numeric values are the actual Kendo MCP schema; verify against
`mcp__kendo__create-issue-tool` if in doubt.

| Type     | Value | Template                                    |
|----------|-------|---------------------------------------------|
| Feature  | `0`   | [User Story](#feature-template-user-story)  |
| Bug      | `1`   | [Bug Report](#bug-template-bug-report)      |
| Task     | `2`   | No fixed template, see [§ Task](#task).     |

| Priority | Value |
|----------|-------|
| Highest  | `0`   |
| High     | `1`   |
| Medium   | `2` (default) |
| Low      | `3`   |
| Lowest   | `4`   |

Attach the issue to its 0.1 epic via `epic_id` (`client-api` = 2, `client-netty` = 3,
`server-netty` = 4, `Release hygiene` = 5).

---

## Feature Template (User Story)

Use for new API surface or new protocol behaviour. Keep the description small enough to hold in
your head; detailed design goes in `/plan-feature` output (`docs/issues/<branch>/PLAN.md`), not
the issue.

For Feature issues, AC describe observable outcomes, not mechanism. "`Client.send` looks up the
codec in a `HashMap`" prescribes implementation; "`Client.send` with an unregistered payload type
throws `IllegalArgumentException` naming the type" describes an outcome. AC written at the
outcome layer stay stable as the implementation evolves.

```markdown
# [Short, descriptive title]

## User Story
As an [API consumer / protocol implementer], I want [functionality] so that [goal].

## Context
[1-2 sentences: why now, which spec section or CLAUDE.md decision this implements.]

## Acceptance Criteria
1. [concrete, testable criterion: API behaviour or wire behaviour]
2. [concrete, testable criterion]
3. [concrete, testable criterion]

## Scope
**In:**
- [in scope]

**Out:**
- [explicitly excluded; prevents scope creep at review]

## What done looks like
- Proof: [an outcome a plain test cannot prove]: [its proof]
- `./gradlew :bend-<module>:test --tests '<fully.qualified.TestClass>' --rerun`
- [which test class(es) gain cases; JaCoCo report shows no new uncovered lines]
```

Aim for **3-5 acceptance criteria**. If you need more, the story is probably too big: split it.

---

## Bug Template (Bug Report)

Use for defects in existing behaviour. The defining feature is the "Problem" + "Cause" pairing:
what is observed, and (when known) why. If the cause isn't diagnosed yet, use the
reproduction-first shape below.

### When cause is known

```markdown
# [Short bug title]

## Problem
[What goes wrong, observable from the API or the wire, in 1-3 sentences.]

## Cause
[Technical analysis: which class, which method, why it fails.
Reference the protocol-v1.md rule it violates if there is one.]

## Acceptance Criteria
- [concrete observable behaviour after the fix]
- [edge case 1]
- [edge case 2]

## Scope
**In:**
- [specific classes/methods that get touched]

**Out:**
- [broader refactors that are tempting but deserve a separate issue]

## What done looks like
- Proof: [an outcome a plain test cannot prove]: [its proof]
- `./gradlew :bend-<module>:test --tests '<fully.qualified.TestClass>' --rerun`
- [the test method that goes red to green, named `method_condition_expected`]
```

Bug AC describe observable state after the fix. Edge cases and regressions belong here too.
`## Cause` names where the failure is (diagnosis, not prescription). `## Scope` names an area of
work ("array-length readers in `ProtocolUtil`"), not a developer instruction.

### When cause is unknown (reproduction-first report)

```markdown
# [Short bug title]

## Problem
[What is observed, in 1-3 sentences.]

## Context
[Which module, which side (client/server), which connection phase: handshake, endpoint
negotiation, keep-alive, endpoint messages.]

## Steps to reproduce
1. [step, ideally the exact bytes sent or the API calls made]
2. [step]

## Expected behavior
[What should have happened, citing protocol-v1.md where it applies.]

## Actual behavior
[What actually happens, including exception type, message and stack trace.]

## Possible causes
[Optional: hypotheses to investigate. Drop the section if you have none.]

## Scope
**In:** [what you think needs to be touched]
**Out:** [what is explicitly out of scope]

## What done looks like
- Proof: the steps above no longer reproduce; a test that replays them is red before the fix.
- [the scoped Gradle command and test class]
```

AC are not written at this stage; they belong in the fix issue once the cause is known. The steps
to reproduce are already an objective check, so `## What done looks like` holds them as its Proof
line.

---

## Task

Tasks use direct description, no fixed template. Describe the work and its motivation, and end
with [`## What done looks like`](#what-done-looks-like), the same section the other types end
with. AC sit at the level appropriate to the task:

- **Refactoring**: structural change is the requirement. AC name the classes, call patterns or
  invariants that must hold afterwards (e.g. "`client-api` has no `io.netty` import").
- **Build / tooling**: AC describe what the build does (`./gradlew build` produces X, a task
  fails on Y), not how it's wired.
- **Research / investigation**: AC describe the deliverable and what makes it actionable (the
  decision recorded in CLAUDE.md, covering these questions, concrete enough to file follow-up
  issues from).

---

## What done looks like

Every issue ends with `## What done looks like`: the checks that tell whoever picks the issue up
(an agent or the developer) that the work is done, without asking anyone. Kendo gives an epic a
list of the same name (`checks`); this section is the issue's own.

```markdown
## What done looks like
- Proof: [an outcome a plain test cannot prove]: [its proof]
- [the scoped Gradle command]
```

- **`Proof:`**: only for an outcome a plain unit test cannot prove, with its proof: a JaCoCo
  report line, a deliberate break that must turn red, bytes captured off a real connection, a
  decision recorded in CLAUDE.md. An acceptance criterion a plain test proves gets no Proof line:
  the Gradle command covers it. A change whose tests prove every criterion has no Proof line.
- **The Gradle command**: the scoped test run the change touches (see
  [Writing Principles](#writing-principles)).
- **At most three `Proof:` lines.** More means the issue is too big, or the lines restate the
  acceptance criteria.
- **No `Browser look:` line.** Upstream's template has one for screens to check in a running app;
  Bend is a library with no UI, so it would always read "none".

An issue written before this section existed ends with `## Testing`; read it as this section.

---

## Writing Principles

### Do

- **English prose.** Technical terms (class names, packet names, Gradle tasks) stay literal.
- **Testable criteria.** "Handles big frames" ✗; "A frame whose length prefix exceeds
  `maximumFrameLength` raises `ProtocolException` before any buffer is allocated" ✓.
- **Concrete values for limits and timeouts.** Name the value (`4 MB` frame cap, `30 s`
  handshake timeout, `16` NUL bytes), not a hedge (`reasonable size`). If it isn't decided yet,
  write `[TBD: <what needs deciding>]` rather than leaving the hedge.
- **Spec traceability.** When an AC implements or enforces a `protocol-v1.md` rule, cite the
  section. When the issue changes the spec, say so in Scope.
- **Concrete test commands.** In `## What done looks like`, name the targeted Gradle command
  (`./gradlew :bend-protocol:test --tests 'net.onestorm.bend.protocol.codec.VarInt28FrameDecoderTest' --rerun`)
  and the test class, not "make sure it works". `./gradlew build` is the cross-module gate;
  name it only when the change spans modules.
- **Design decisions in PLAN.md / DECISIONS.md**, not the issue body.
- **Scope in two columns.** `**In:**` and `**Out:**`; the `Out:` list prevents scope creep.
- **Right-size.** One PR per issue. Bigger: split it and group the parts under the epic.
- **Code-fences** for identifiers (classes, packets, Gradle tasks).

### Avoid

- Implementation details in the body, unless a bug's cause requires them.
- A shallow `## What done looks like`, such as "make sure it works" or "all tests keep passing".
- Vague criteria ("must be fast", "must be robust").
- Defensive checks for cases the code can't produce as AC: an unreachable branch is deleted,
  not specified (see CLAUDE.md, "Testing / coverage").

### Title Conventions

- Name the symptom or the capability, not the proposed fix.
- Good: `Decoder allocates arrays sized by an unchecked length prefix` (symptom).
- Also good: `Client can register an endpoint handler at runtime` (capability).
- Avoid: `Fix decoder bug` (no symptom, no capability, useless in search).

---

## Examples

### Feature

```markdown
# Client advertises its supported endpoints after receiving Available Endpoints

## User Story
As an API consumer, I want the client to tell the server which endpoints I have handlers for so
that the server only routes messages I can handle.

## Context
Implements the Supported Endpoints flow in protocol-v1.md. CLAUDE.md "In-flight work (client)"
decided to resend the whole set (not a delta) on each trigger.

## Acceptance Criteria
1. After an `Available Endpoints` packet arrives, the client sends one `Supported Endpoints`
   packet containing exactly the ids that are both available and registered locally.
2. Registering a handler for an id already in the available list triggers a new
   `Supported Endpoints` packet with the full updated set.
3. Registering a handler for an id the server hasn't advertised sends nothing.
4. `Supported Endpoints` goes out on the built-in packet path, never through `Client.send`.

## Scope
**In:**
- `client-netty` handshake/endpoint handler
- the registry hook in `client-api` that signals a new registration

**Out:**
- unregister handling (separate issue)
- server-side acknowledgement timeout

## What done looks like
- `./gradlew :bend-client-netty:test --tests 'net.onestorm.bend.client.netty.SupportedEndpointsTest' --rerun`
- `EmbeddedChannel` tests asserting the outbound packets, e.g.
  `availableEndpoints_registeredSubset_sendsIntersection`
```

### Bug (cause known)

```markdown
# Array readers allocate before checking the buffer holds the claimed length

## Problem
A peer can send a small frame whose array-length VarInt claims ~2^31 elements; the decoder
attempts a multi-gigabyte allocation and the process dies with `OutOfMemoryError`, which the
decoder's `catch (Exception e)` does not catch.

## Cause
`ProtocolUtil.readVarIntArray` and `AvailableEndpointsPacket.readEndpointArray` call
`new int[length]` / `new Endpoint[length]` straight from the length prefix, with no
`readableBytes()` check. `readString` already guards this; the array readers don't.

## Acceptance Criteria
- An array length greater than the buffer's readable bytes raises `ProtocolException` without
  allocating.
- A negative array length raises `ProtocolException`.
- Valid arrays, including empty ones, still decode unchanged.

## Scope
**In:**
- array-length readers in `ProtocolUtil` and `AvailableEndpointsPacket`

**Out:**
- requiring canonical VarInt encoding (declined, see CLAUDE.md)

## What done looks like
- `./gradlew :bend-protocol:test --tests 'net.onestorm.bend.protocol.util.ProtocolUtilTest' --rerun`
- red to green: `readVarIntArray_lengthExceedsReadable_throws`
```
