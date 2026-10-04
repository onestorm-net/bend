# Extraction inventory: Phase 1 categories

Phase 1 in SKILL.md is the gate: every PLAN.md item must land in one of these
categories. This file holds the full category list and the "what fits where"
rules. Load it the moment you start the inventory.

The point of an explicit inventory, instead of "read the plan and write
tasks", is to make scope creep and gaps visible **before** task wording
locks them in. A missing category at this stage costs an extra round of
revisions; a missing category after `/next` starts costs a re-plan.

## The literal inventory

Copy this block to the developer with every line filled in. Write "none" for
an empty line rather than deleting it, so a skipped category is visible.

```markdown
## Extraction Inventory

### Packet / codec changes (`protocol`)

- [ ] Packets to add or modify: [class, packet id, field layout]
- [ ] Codec changes: [frame decoder/encoder, PacketDecoder/PacketEncoder, ProtocolUtil readers/writers]
- [ ] Registration changes: [ProtocolV1 / AbstractProtocol packet table]
- [ ] Encoder/decoder symmetry: [every new write path has a matching read path with the same width/limits]

### Spec changes (`protocol/protocol-v1.md`)

- [ ] Sections to add or amend: [section name + the MUST/SHOULD being added]
- [ ] Constants introduced or changed: [timeouts, size caps, type values]
- [ ] Spec statements the code must now enforce: [each one, paired with its enforcing code]

### Public API (`client-api`) types and Javadoc

- [ ] Interfaces / value types to create: [fully qualified names]
- [ ] Interfaces / value types to modify: [and whether the change is breaking]
- [ ] Javadoc to write: [every new public type and member]
- [ ] Netty leakage check: [no `ByteBuf` or other Netty type in a public signature; `java.nio.ByteBuffer` at the boundary]

### Pipeline wiring / handlers (`client-netty`, `server-netty`)

- [ ] Handlers to add or modify: [class, position in the pipeline]
- [ ] Send paths: [built-in packet path vs. public endpoint-payload path, kept separate]
- [ ] Endpoint registration / routing: [register, unregister, advertise, withdraw]
- [ ] ByteBuf ownership: [who retains, who releases, at which boundary]

### Disconnect conditions and `ProtocolException` paths

- [ ] Disconnect conditions: [each one from the plan, with its `Disconnect` Type value]
- [ ] Malformed / hostile input: [each length prefix, count, or id read from the wire and the check that bounds it]
- [ ] New throw sites: [`ProtocolException` for wire input, `IllegalArgumentException` for caller misuse]

### Threading / lifecycle

- [ ] Event-loop work: [anything that could block the Netty event loop]
- [ ] Timeouts: [handshake, acknowledgement, keep-alive, and what fires on expiry]
- [ ] Lifecycle transitions: [connect, handshake, ready, disconnect, close; unregister with messages in flight]

### Test plans

- [ ] Unit tests: [test class per production class, behaviours to cover]
- [ ] Round-trip tests: [client to server scenarios that cross the wire]
- [ ] Coverage expectations: [classes whose JaCoCo XML should show no uncovered lines, and any line expected to stay uncovered with the reason]
- [ ] Plan's verification steps: [from the plan's verification section]
```

## What lands where

| If PLAN.md says… | Inventory category | Eventually becomes (Phase 2) |
|---|---|---|
| New packet class or field layout | Packet / codec changes | `**Touches:**` of the `protocol` task |
| "Readers MUST…" / new constant in the spec | Spec changes | Same task as the code that enforces it |
| New public interface or value type | Public API types and Javadoc | `client-api` task |
| New `ChannelHandler`, pipeline order change | Pipeline wiring / handlers | `client-netty` / `server-netty` task |
| "If the peer does X → disconnect with Type N" | Disconnect conditions | Task Context "Watch out" + a `[RED]` test |
| Length prefix or count read from the wire | Disconnect conditions → hostile input | "Watch out" + a `[RED]` test with an oversized claim |
| "Times out after 30 s" | Threading / lifecycle → timeouts | "Watch out" + a `[RED]` test on the timeout path |
| Client and server both involved | Test plans → round-trip tests | Round-trip test task (or the later of the two module tasks) |
| Plan's "Verification" section steps | Test plans → verification steps | Final verification task |

If a PLAN.md item doesn't fit any row, **the plan has a gap**; surface it to
the developer before continuing. Don't shoehorn ambiguous items into a code
category to keep the inventory looking complete; that's how scope creep ships.

## Coverage check (Phase 3)

After tasks are drafted, the coverage check is the symmetric output of this
inventory: every box ticked above must trace forward to a task. The check
itself lives in SKILL.md Phase 3 because it's a fail-closed gate; the
rationale lives here:

A coverage check is not "did I list everything?" (that's the Phase 4 coverage
gate). It's "did the tasks I just wrote actually reflect the inventory
I just built?" Two gates, both fail-closed, because losing an item between
inventory and task wording is the most common mid-implementation surprise.
