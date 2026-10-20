# PLAN.md template

Save the plan to `docs/issues/<branch-name>/PLAN.md`, where `<branch-name>` is the exact current
branch (`BD-NNNN-<kebab-slug>`). Use this exact structure: the downstream agents and skills
(`plan-reviewer`, `surface-reviewer`, `/task-writer`, `/next`, `precedent-reviewer`) parse the
section names, so don't rename the ones you keep.

The plan has two readers, and every section is sized for them: the implementing session, which
often starts after a `/clear` with nothing but `CLAUDE.md` and this file, and `precedent-reviewer`
at PR time, which holds the Security & Cost Surface prose against the diff. A section that
neither reader needs does not go in. What the planner needed to *reach* the plan (the gap table,
the pre-flight resolver output) stays in the planning conversation. So does everything the
reviewers produce: `plan-reviewer` and `surface-reviewer` report to the session, and the plan
carries no record of their verdicts. A plan that passed is the record.

One section is conditional: `## Sweep Inventory` appears only when the plan adds a cross-cutting
rule. Otherwise the heading is omitted rather than carrying an `N/A` row.

```markdown
# <branch-name>: <name>

**Date:** <YYYY-MM-DD>
**Status:** Draft
**Issue:** [BD-NNNN](https://onestorm.kendo.dev/projects/1/issues/BD-NNNN)
**Decisions:** [DECISIONS.md](/docs/issues/<branch-name>/DECISIONS.md)

## Goal
<one sentence: what this enables for a user of Bend (a client or server author, or a peer on the wire)>

## Key Design Decisions

Short table of every structural choice the developer should review. This section exists so the
developer can catch convention violations in 30 seconds without parsing the full plan.

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| <what needed deciding> | <what you chose> | <file:line, `Class#member`, or CLAUDE.md section that proves this matches the codebase> |

Examples of what belongs here:
- Packet shape: "Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` class implementing `Packet`, registered in `ProtocolV1`" → reference `KeepAlivePacket.java`
- Bad-input exception: "`ProtocolException` on decode, `IllegalArgumentException` for a caller passing an out-of-range value at setup" → reference CLAUDE.md § API design decisions and `AbstractProtocol`
- Payload type at the API boundary: "`java.nio.ByteBuffer`, converted in `client-netty`" → reference CLAUDE.md § API design decisions
- Construction: "implementation builder (`NettyClient.builder()...connect()`) returning the `Client` interface" → reference CLAUDE.md § API design decisions and ADR 0018

**Every choice must cite a concrete codebase reference.** If you can't point to an existing
file or a `CLAUDE.md` decision that proves this matches the conventions, flag it as a deliberate
deviation and explain why.

**Layer compliance:** For each new class, note which module it lives in and confirm that
module's `build.gradle.kts` dependencies and the CLAUDE.md layer rules allow it to depend on
everything it needs (no Netty types in `client-api`; `client-api` depends on nothing;
implementation modules depend only on their api module and `protocol`).

## Scope
- **In scope:** <explicit list>
- **Out of scope:** <explicit list: things confirmed we're NOT building>

## Approach
<which files to create/modify, in what order, grouped by module (`protocol` first when the wire
changes, then the api module, then the `-netty` implementation)>

## Sweep Inventory

<Only when the plan adds a cross-cutting rule: a length check every array reader must run, a
validation every packet's decode must call, a disconnect every timeout path must send, a release
every handler that consumes a `ByteBuf` must do. Omit the heading otherwise.>

Enumerate the sibling sites mechanically and mark each one. The site list comes from the
Phase 1.4 grep, not from memory: cite the pattern searched.

| Rule added | Sibling site (grep hit) | Status |
|---|---|---|
| <the rule> | <file / class / method the rule must reach> | Applied / Skipped: <reason a reviewer can check> |

An unmarked row blocks Phase 4d: review keeps finding site N+1 of an N-site sweep, and this
table is where site N+1 gets found first.

## Security & Cost Surface

Carried forward from Phase 1.6. Seven rows; each row is a **prose answer** to the row's questions ending in a `Proof:` line, or `N/A: <one-line reason>`. Audited at Phase 5 by `surface-reviewer` (running in parallel with `plan-reviewer`).

The canonical questions for each row live in [`surface-questions.md`](/.claude/skills/plan-feature/references/surface-questions.md); fill prose here that answers those questions, don't paraphrase them back.

### Untrusted wire input and allocation bounds
<prose: each field read off the wire and which side decodes it, the check that runs before every peer-sized allocation or loop, reader/writer width symmetry, the exception malformed input raises, spec MUST validation on decode; OR `N/A: <reason>`>

### Connection lifecycle, timeouts and silent peers
<prose: states added or changed with happy path and failure walk, the timeout bounding every wait (value, spec rule, mechanism, cancellation on close), which side disconnects with which `Disconnect` type, what the API caller observes; OR `N/A: <reason>`>

### Threading and the event loop
<prose: which thread runs each new piece of code, anything that can block the event loop (including user handlers), shared state and what makes it safe, how public methods hand off to the event loop and preserve ordering; OR `N/A: <reason>`>

### Resource ownership
<prose: owner and release of every `ByteBuf` on success and failure paths, copy vs view at the `ByteBuffer` boundary, the bound on every per-connection growth, a listener on every `ChannelFuture`, what is freed on close; OR `N/A: <reason>`>

### Spec conformance
<prose: the `protocol-v1.md` rules touched (quoted by section), where each is enforced on each side, the exact spec edit and compatibility/version impact if the wire or a rule changes, no unreachable defensive checks; OR `N/A: <reason>`>

### Public API surface, module boundaries and enforcement
<prose: new public types and methods (no Netty types), module dependency changes, separate built-in and endpoint send paths, caller-misuse behaviour, Javadoc on new public types, the enforcement level (L1 compile time / L2 test / L3 runtime / L4 review with trade-off) of each new structural rule; OR `N/A: <reason>`>

### Runtime endpoint add and remove
<prose: the update cycle for add and remove on a live connection, in-flight messages on a removed endpoint, overlapping updates and revisions, handling of unknown or wrong-direction messages, supported-set recomputation; OR `N/A: <reason>`>

## Acceptance Criteria

Verifiable conditions that must ALL be true for this feature to be complete.
QA will check each criterion against the implementation.

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | <observable outcome: binary pass/fail> | <how to verify: the test and the `./gradlew` command that runs it, or the bytes to send and the response to expect> |
| 2 | ... | ... |

## Reuse and patterns

<One list. Each line is a file path plus what the implementer takes from it: a helper or class
to **call**, or a sibling whose **shape to mirror**. This is the only place a session that starts
cold learns which existing code the plan was built on; the Approach names the files to create or
edit, not the files to copy from.>

- `<path>`: call: <what it provides>
- `<path>`: mirror: <what shape to copy>

Write "no reuse: building from scratch because X" when there is genuinely nothing; never leave
the section empty or say "follow project conventions".

## Testing Strategy

<Per-PR test tables only. The standing rules (JUnit Jupiter + AssertJ, JaCoCo reporting, the
coverage expectations) live in `CLAUDE.md`, which every session loads; do not restate them here.>

<for each PR, include a table with test class names, test method names and behavioral descriptions>
<test method names follow `method_condition_expected`, e.g. `decode_arrayLengthExceedsReadable_throws`,
`getPacketId_unregisteredClass_throws`; never "should...">
<3-6 tests per class under test; more usually means you're testing implementation>
<each test states its red case: "fails when ___". A test whose red case can't be stated is
decoration: a round-trip through the same buggy reader and writer, a threading test on an
`EmbeddedChannel`, a timeout test that never advances time, all pass whatever the code does>
<every new guard or limit ships a test that feeds it the input it must reject (a length prefix
larger than the readable bytes, a 17-byte NUL run, a zero-length frame, a namespaced id with an
upper-case letter)>
<every new test class names the Gradle command that runs it:
`./gradlew :bend-<module>:test --tests '<fully.qualified.TestClass>' --rerun`>

## Documentation Sync

Every row is marked **Update** (with what changes) or **N/A: <reason>**; "checked, nothing to
update" is a row, not an omission.

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | |
| New or changed public type or method in `client-api` (or `server-api`) | Javadoc on that type | |
| A design decision, layer rule, convention or module plan this feature makes, changes, deletes or renames | `CLAUDE.md` (one line under the matching heading, root or module) plus the reasoning in this issue's `DECISIONS.md`, or an update to the ADR whose Decision changes | |

This table asks **which** documents change. What they then *claim* is checked after the fact
by `correctness-reviewer`'s promised-behaviour rule (Sins of omission), which runs on every
branch.

## Edge Cases
<cases from the Q&A: how each is handled>

## Risks
<what could go wrong, what to watch for during implementation>
```
