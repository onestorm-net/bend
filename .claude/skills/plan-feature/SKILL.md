---
name: plan-feature
description: |
  Structured feature planning workflow: interrogate the developer with hard, codebase-informed
  questions until requirements are crystal clear, then produce PLAN.md and DECISIONS.md. Use when
  planning a feature, designing a feature, "how should we build this", "let's plan", "I want to
  build X", or any request that needs architectural thinking before implementation. If a change
  spans more than one module (protocol, client-api, client-netty, server-netty) or changes the
  wire spec (protocol/protocol-v1.md), trigger this skill before coding.
---

# Plan Feature

You are entering interrogation mode. Your job is to deeply understand what needs to be built
before any code gets written. You will grill the developer with hard, specific questions informed
by the actual codebase, not generic product-manager questions.

The goal: by the end of this process, you and the developer have a shared, precise understanding
of what's being built, what's NOT being built, and how it should work in edge cases. Then you
produce the plan yourself: you have the deepest context, having read the code and run the
entire conversation.

Bend facts this skill relies on: tracker is Kendo via the local `kendo` MCP server
(`mcp__kendo__*` tools only, never `mcp__claude_ai_Kendo-*`), `project_id: 1`, issue keys
`BD-NNNN`, issues written in English. Planning artifacts live in `docs/issues/<branch-name>/`
(see [`.claude/references/plan-directory.md`](../../references/plan-directory.md)). Gradle is
always `./gradlew`; Gradle project paths are `:bend-protocol`, `:bend-client-api`,
`:bend-client-netty`, `:bend-server-netty`.

## Phase 0: Resolve the branch and issue

Planning happens on the issue's branch, and the plan folder is named after it.

1. Run `git branch --show-current`. The branch must match `BD-NNNN-<kebab-slug>` (e.g.
   `BD-0012-server-endpoint-registry`). If it doesn't (you're on `development` or `main`, or on a branch with no
   issue key), stop: *"plan-feature writes into `docs/issues/<branch-name>/`, so it needs an issue
   branch. Run `/newbranch` (new issue) or `/prepare-issue BD-NNNN` (existing issue) first."*
2. The issue key is the branch's `BD-NNNN` prefix. If the developer passed an issue URL
   (`https://onestorm.kendo.dev/projects/1/issues/BD-0012`) or key as the argument and it differs
   from the branch's key, stop and ask which one is meant.
3. Read the issue with `mcp__kendo__prepare-issue-context-tool(issue_key: "BD-NNNN")`. It returns
   the issue (with its real database id) and its epic.

**Issue key ≠ issue ID.** `BD-0012` does NOT mean the database id is 12. Never extract the number
from a key and use it as an id.

If the repo has a `CONTEXT.md` glossary at its root, read it before grilling: it defines the
canonical domain terms and the aliases to avoid.

## Phase 1: Research before asking

Read `CLAUDE.md` at the repo root and the module `CLAUDE.md` of every module the feature
touches (e.g. `protocol/CLAUDE.md`), plus `docs/decisions/README.md` and the ADRs those rules
link for the areas the feature touches. CLAUDE.md holds the rules; the ADRs hold the reasoning
and the rejected alternatives. Only an `Accepted` ADR's Decision section binds: never plan
toward a `Deferred` or `Open` ADR. Read `protocol/protocol-v1.md` whenever the feature touches
anything on the wire.

If the repo has a root `CONTEXT.md` glossary, it is the vocabulary contract for this whole
session. If the developer uses a term that conflicts with the glossary, call it out and resolve
it; if the feature surfaces a new term that isn't defined, add it to `CONTEXT.md` the moment it's
resolved.

Then do the following:

### 1a. Check the tracker board

- Search for related issues with `mcp__kendo__search-issues-tool` (`project_id: 1`).
- Read `kendo://projects/1/epics` to check for relevant epics. The 0.1 epics are `client-api`,
  `client-netty`, `server-netty` and `Release hygiene`; the feature almost always belongs to one.
- Read each related issue via `mcp__kendo__prepare-issue-context-tool` to understand the scope
  before asking questions the board already answers.

### 1b. Find the closest existing feature

Before designing anything, find the code in the repo that most closely resembles what the
developer wants to build. Trace it through the full chain; this becomes the blueprint:

- **Inbound:** `VarInt28FrameDecoder` → `PacketDecoder` → packet class → pipeline handler →
  endpoint registry / dispatch → public API (`client-api`)
- **Outbound:** public API → `PacketEncoder` → `VarInt28LengthEncoder`
- **Spec:** the matching section of `protocol/protocol-v1.md` (Frame, Flow, the packet's own
  section, Disconnect conditions)

Use Glob/Grep to find the candidate. For example, if the developer wants a new built-in packet,
look at how `KeepAlivePacket` is defined, registered in `ProtocolV1`, and tested in
`protocol/src/test/java/`; if they want a new length-prefixed array field, look at
`AvailableEndpointsPacket#readEndpointArray` and `ProtocolUtil#validateArrayLength`.

Once you find it, read the key files and note:
- How the packet or codec is structured (read/write symmetry, where validation lives)
- Which exception it throws on bad input and why (`ProtocolException` vs
  `IllegalArgumentException`)
- Where the matching spec text lives and what MUST rules it states
- What tests exist and how they're structured (names, AssertJ style, `ByteBuf` release)

This existing code becomes the reference implementation in your plan. Every question you ask
and every decision in the plan should be framed relative to it: "`KeepAlivePacket` does X:
should this work the same way, or do we need something different here?"

### 1c. Audit existing code for reuse

This step is non-negotiable. `protocol` already has helpers for most wire needs, and building
a second VarInt reader or array reader is how the frame-decoder width bug and the array
pre-allocation bug happened in the first place.

Browse, don't guess from memory:
- `protocol/src/main/java/net/onestorm/bend/protocol/util/ProtocolUtil.java`: VarInt, string and
  array read/write, `validateArrayLength`
- `protocol/src/main/java/net/onestorm/bend/protocol/codec/`: framing and packet codecs
- `protocol/src/main/java/net/onestorm/bend/protocol/endpoint/Endpoint.java`: endpoint id and
  flag validation
- `protocol/src/main/java/net/onestorm/bend/protocol/exception/ProtocolException.java`
- Whatever `client-api`, `client-netty` and `server-netty` contain by now

Make a concrete list of what you found. This list goes into Phase 2 questioning: present it
to the developer and confirm: "I found these existing pieces we can reuse. Anything I'm
missing, or should any of these be replaced instead of reused?"

### 1d. Sketch module shape (deep vs shallow): apply the test, don't grade yourself

Before validating layers, decide what new classes this feature introduces and whether each is deep or shallow. Load [`references/module-shape-lens.md`](references/module-shape-lens.md) and read the **shallow-detection test** section in particular. The lens is three specific tests about interface complexity vs implementation per use case, not a vibe check. Implementation line count is not one of them: a handler with 200 lines of state-machine code inside, one method, and 5 primitive params is shallow regardless of body length.

For each in-scope class, output one row to PLAN.md's "Key Design Decisions" table (or a dedicated Module-Shape sub-table) with these columns:

- **Inputs/outputs**
- **Hidden behaviour**
- **Test seam**
- **Shallow-test reckoning**: explicitly answer: "if a plausible second use case lands, do I add a *method* or a *parameter*?" Method ⇒ shallow-and-suspect. Parameter ⇒ likely deep.
- **Verdict**: deep / shallow-but-justified / shallow-and-suspect

The Shallow-test reckoning column is not optional. A row that says "Deep" without showing the reckoning is treated as missing for Phase 1.5 gap-analysis purposes.

Fold or expand any shallow-and-suspect class before continuing. The two fixes (demote into the caller, or promote to protocol primitives) are documented in the lens reference. Plan-time is the last cheap moment to fix this: no pre-PR reviewer grades module depth on its own, and a shallow class only surfaces later as the sibling drift and dead scaffolding `precedent-reviewer` flags, by which point the shape is already built.

### 1e. Validate against the layer rules

**Before proposing where classes live, check the layer rules.** Bend has no architecture tests;
the rules live in `CLAUDE.md` (Module naming convention, Module set, API design decisions,
Protocol architecture) and the module `CLAUDE.md` files, with reasoning in the linked ADRs;
they are the source of truth. Read them, don't rely on this summary:

- `client-api` (and the future `server-api`) exposes no Netty types: no `ByteBuf`, no
  `Channel`, no `ChannelFuture`. Use `java.nio.ByteBuffer` at the boundary and convert inside
  the `-netty` module.
- `client-api` depends on nothing. Implementation modules (`client-netty`, `server-netty`) may
  depend on their api module and on `protocol`. Check the module's `build.gradle.kts`
  dependencies block before proposing an import across modules.
- The built-in packet path (Handshake, Disconnect, Available/Supported Endpoints and their
  acknowledgements, Keep Alive) and the endpoint-message path stay separate: built-ins are
  written to the channel directly, never through the public send path.
- `ProtocolException` for malformed or oversized wire input; `IllegalArgumentException` /
  `IllegalStateException` for caller misuse at setup time.
- `server-netty`'s register/unregister/handler signatures stay Netty-agnostic, as if they were
  already in `server-api`.
- A `Client` is built by the implementation's own builder (e.g. `NettyClient.builder()...connect()`),
  whose `connect()` returns the `client-api` `Client` interface; the concrete class stays
  package-private. `client-api` itself has no `connect()` and no settings.

For every new class in the plan, verify: "Does this class's module allow it to depend on
everything it needs?" If not, move it, even if it feels semantically off.

Include findings in the plan: "Proposed class X in module Y: verified `Y/build.gradle.kts` and the
CLAUDE.md layer rules allow Y to depend on [list]."

## Phase 1.4: Pre-flight verification (mandatory, fail-closed)

Every path and symbol the plan cites gets resolved mechanically before it lands. Projecting what *should* exist instead of grepping once cost a plan three reviewer rounds: [`references/anti-patterns.md`](references/anti-patterns.md) § Calibration records the four categories it produced.

`<skill dir>`, used here and again by Phase 5's reviewer agents, is the checked-in copy:

```bash
skill_dir="$(git rev-parse --show-toplevel)/.claude/skills/plan-feature"
```

Collect every path and symbol you're about to cite, one per line, and run them through the resolver:

```bash
printf '%s\n' 'protocol/src/main/java/net/onestorm/bend/protocol/codec/VarInt28FrameDecoder.java' \
    'ProtocolUtil#validateArrayLength' 'net.onestorm.bend.protocol.endpoint' \
  | bash "$skill_dir/scripts/verify-citations.sh"
```

It resolves paths against the repo root and each module's `src/main/java/` and `src/test/java/`, resolves `Class#member` / `Class.member` by finding `Class.java` and checking the member is named in it, resolves qualified names as package directories or class files, greps bare symbols across the module source trees, `gradle/` and the root build scripts, and tolerates a trailing line reference (`VarInt28FrameDecoder.java:40-52`). It never searches `build/`, `.gradle/`, `.idea/`, `.claude/`, `docs/` or Markdown files for symbols. It reports on every citation before exiting, so one pass gives you the whole list. `CITATION_PATH_PREFIXES` / `CITATION_SEARCH_ROOTS` override the defaults (documented in the script header). **A `MISSING` line is a fabricated citation: fix it or drop the claim. You may not proceed past Phase 1.4 with a non-zero exit.**

Its own tests run with `bash "$skill_dir/scripts/verify-citations.test.sh"`; run them after editing the script.

Three claim shapes the script can't resolve, which you still owe by hand:

| Check | What | How |
|---|---|---|
| **Framework / library behaviour** | Every claim of the form "Netty does X" (`ByteToMessageDecoder` never calls `decode()` with an empty buffer, a handler's `channelInactive` fires after `exceptionCaught`, `writeAndFlush` releases the message on failure) | Cite the Netty 4.x class and method, ideally as a GitHub URL at the release tag `gradle/libs.versions.toml` pins (`https://github.com/netty/netty/blob/netty-<version>/<path>#L<line>`), or weaken the claim to "verify at implementation time." The resolver reports Netty classes as MISSING on purpose: they aren't in this repo. No vibes on framework behaviour. |
| **Symbol-removal blast radius** | Every symbol you plan to delete or rename (packet class, codec, `ProtocolUtil` method, public API type, spec term) | `grep -rn <symbol>` across every module's `src/`, `protocol/protocol-v1.md`, every `CLAUDE.md` and `docs/decisions/`, and **inspect every hit**, not just the ones from the obvious feature path. The third callsite always lives somewhere unexpected (often a test helper, or the spec text). |
| **Added-rule sweep inventory** | Every cross-cutting rule the plan **adds**: a length check every array reader must run, a disconnect every timeout path must send, a validation every packet decoder must call, a release every handler that consumes a `ByteBuf` must do | `grep -rn` the sibling population (every packet class, every reader in `ProtocolUtil`, every handler) and carry **every hit** into PLAN.md's `## Sweep Inventory`, each marked Applied or `Skipped: <reason>`. The blast-radius row covers what you remove; this row covers what you add. The array pre-allocation bug is the shape: `readString` had the guard, the two array readers didn't. |

Reviewers catch unverified confident claims; they will NOT reliably catch hedged ones. So anything you couldn't verify gets written as "verify at implementation time", not asserted.

## Phase 1.5: Gap analysis (mandatory, fail-closed)

Before any drafting, produce this literal checklist and mark each row **✓ Covered**, **? Partial**, or **✗ Missing**. Every ✓ requires a quoted source. **No ? or ✗ row may survive into drafting**: Phase 2 exists to close them, so an open row sends you there rather than blocking you. Phase 3 is what's unreachable while any row is still ? or ✗.

Load [`references/quality-gates.md`](references/quality-gates.md) for the rationale, sycophancy guards, and proceed/return rules.

| Required for a plan | Marker | Source (must quote / cite) |
|---|---|---|
| **Goal**: one sentence the developer would re-read and agree with | ✓ / ? / ✗ | issue body / first prompt (quote the line) |
| **Acceptance criteria**: testable, observable, distinct from the goal (≥ 3) | ✓ / ? / ✗ | issue body (quote each, or mark missing) |
| **In-scope**: explicit file list or module boundary | ✓ / ? / ✗ | issue body / prompt / Phase 1b research |
| **Out-of-scope**: at least one explicit non-goal | ✓ / ? / ✗ | usually missing; interrogate (`Deferred` ADRs such as 0017, the module system, are a common source of things to rule out, never of things to build toward) |
| **Edge cases**: malformed or hostile input, timeouts, disconnect mid-flow, concurrent register/unregister, limits | ✓ / ? / ✗ | usually missing; interrogate with concrete cases drawn from Phase 1b |
| **Architecture fit**: which existing code does this resemble; what gets reused | ✓ / ? / ✗ | Phase 1b/1c findings (cite file paths) |
| **Module shape**: every new in-scope class has the **shallow-test reckoning** filled in (not just a "Deep" label); any shallow-and-suspect class has been demoted or promoted | ✓ / ? / ✗ | Phase 1d output; a row that says "Deep" without showing the "method or parameter for the next use case?" reckoning counts as ✗, not ✓ |
| **Risk / uncertainty**: what could go wrong, what's unknown | ✓ / ? / ✗ | usually missing; interrogate |

Output the table verbatim to the developer with marks and citations filled in. It stays in the planning conversation: it is a gate on the planner, and the sections it proves complete (Goal, Acceptance Criteria, Scope, Edge Cases, Reuse and patterns) are what PLAN.md carries. A cold implementing session needs those sections, not the proof they were gathered.

**Phase 1.6 comes next either way; it is never skipped.** After it:

- **All ✓**: skip the Phase 2 interrogation and go to Phase 3. The input is genuinely complete.
- **Any ? or ✗**: go to Phase 2, targeting only those rows. Don't sweep.

## Phase 1.6: Security & Cost Surface (mandatory, fail-closed)

Produce a `## Security & Cost Surface` section in PLAN.md with **seven prose paragraphs**, each answering the questions for one row and ending with its Proof line, or `N/A: <one-line reason>` when no question on the row applies. **You may not proceed past Phase 1.6 with any unanswered question on a populated row.**

The canonical questions and worked examples live at [`references/surface-questions.md`](references/surface-questions.md); load it now. It is the single source of truth shared with the `surface-reviewer` agent at Phase 5. The rows are deliberately question-shaped, not field-shaped, so they generalise to feature shapes not seen yet. The hazard classes they ask about come from Bend's own history, recorded in the Context of the ADRs in `docs/decisions/` (the frame-decoder width asymmetry in 0001, the array pre-allocation bug in 0004, the built-in packets sent through the public send path in 0008, the unreachable "defensive" checks in 0009): a row whose hazard the feature repeats needs an answer that names the seam that ends it, not a generic one.

Unit tests of individual packets do not cover this. They cover *code shape*, not the flow of untrusted bytes into an allocation, what a silent peer does to a connection, which thread a handler runs on, or who releases a buffer. [`references/quality-gates.md`](references/quality-gates.md) carries the rationale and the sycophancy guards (paraphrasing the questions back is THIN, not OK; a feature that reads anything off the wire cannot mark Row 1 N/A).

Output the seven paragraphs to the developer for confirmation, then carry them into PLAN.md at Phase 4c. The `surface-reviewer` agent grades them at Phase 5, and `precedent-reviewer` holds them against the diff at PR time: this is the one part of PLAN.md with a reader after the branch is built.

## Phase 2: Interrogate

Now ask questions, **targeting only the ? and ✗ rows from Phase 1.5**. No generic sweep.

The mode is **interview with hypotheses**: for every question, propose your recommended answer with one-line rationale, and let the developer confirm or correct. The developer should be reacting to a stance, not authoring answers from cold. A multiple-choice quiz with no opinion attached makes the dev think from zero; that's a worse interview, not a politer one.

### Order of attack

Resolve in dependency order, not gap-table order. A late-discovered wire-format change invalidates state-machine and API decisions made earlier; that's the most expensive form of mid-interview rework.

1. **Wire format & spec**: which packets and fields exist, their encoding, size limits, which MUST and disconnect rules in `protocol-v1.md` change
2. **State machine & threading**: connection states and transitions, timeouts, what runs on the event loop and what doesn't, ordering against in-flight endpoint messages
3. **Public API**: what `client-api` (or `server-netty`'s future-public surface) exposes, its types, and what stays internal
4. **Failure modes**: malformed input, silent or half-open peers, disconnect mid-flow, register/unregister races, resource exhaustion

Don't ask an API question before the wire format is settled. If a row in the gap table is downstream of an unanswered upstream row, defer it.

### Self-serve before asking

Before sending an `AskUserQuestion`: can the codebase answer this? If yes, **answer it from the code and ask for confirmation**, not for the answer. "`ProtocolUtil#readString` checks `buffer.isReadable(length)` before reading: confirming the new reader uses `validateArrayLength` the same way?" is one click. "Should we validate the length?" wastes the dev's attention on something we already know.

If the dev makes a claim about how the code behaves and you suspect it's wrong, **check the code before continuing**. Surfacing a contradiction ("you said the decoder accepts 5-byte lengths, but `VarInt28FrameDecoder` caps at 4 bytes / 28 bits: which is right?") is more valuable than tactfully agreeing. The same goes for the spec: if the dev's description contradicts a MUST in `protocol-v1.md`, say so.

### First round: present what you found

Your first question round should always present what you discovered in Phase 1. Before asking
about scope or behavior, show the developer:

1. **The reference code** you found: "I looked at how [`KeepAlivePacket` / `AvailableEndpointsPacket` / the frame decoder] works.
   It follows [pattern]. I'd like to use that as the blueprint. Sound right?"
2. **Shared code you plan to reuse**: "I found these existing pieces: [list]. Anything missing
   or anything you'd rather rebuild?"
3. **The spec sections involved**: "This touches `protocol-v1.md` § [Flow / Disconnect conditions /
   the packet's section]. I'd change [these lines] / leave the spec unchanged. Does that match?"

This grounds the conversation in the actual codebase and catches "we already have that" early.

### Rules for questioning

- **Every round goes through `AskUserQuestion`**: up to 4 questions per call, each with 2–4 concrete options drawn from the codebase. The developer clicks; they don't parse paragraphs or type prose. Use previews for API-shape choices (method signatures, builder vs constructor).
- **Lead with a stance.** Each question's description carries "I'd pick X because Y": the dev reacts to a recommendation, not a blank quiz.
- **Push back on vague answers.** "It should be flexible" or "whatever makes sense" is not an answer; ask for the concrete case behind it.
- **Probe relationships with a specific story**, not an abstract category: "The server unregisters `bend:chat` while a client's `Endpoint Message` on it is already decoded and queued for the handler. What does that handler see, and what does the client see?" surfaces boundary disagreements that "what about race conditions?" doesn't.
- **Live glossary check.** If the repo has a root `CONTEXT.md`, an answer using a term that conflicts with it stops the round: surface the conflict, propose the canonical term, update `CONTEXT.md` inline. Don't let domain language drift mid-conversation.
- **Restate after every round** in 2-3 lines: "Agreed so far: A → X, B → Y. Open: Z." In-flight alignment means Phase 3 is a final sanity check rather than the only moment the developer can object.
- **Keep text between rounds under 5 lines.** Summaries and context, not essays.

### When to stop asking

Stop when ALL of the following are true, however many rounds that takes:
- You know exactly what goes on the wire and what the spec says about it
- You know what a caller of the public API sees and calls
- You know the scope boundaries (in AND out)
- You know how each failure mode is handled (and which side disconnects with which `Disconnect` type)
- You've identified which existing patterns to follow
- You can write at least 3 verifiable acceptance criteria that the developer agrees with

## Phase 3: Confirm understanding

Before producing the plan, write a summary and get explicit confirmation:

```
Here's my understanding:

**What we're building:** [one sentence]
**In scope:**
- [explicit list]

**Out of scope:**
- [explicit list]

**Key decisions:**
- [each non-obvious decision from the Q&A]

**Edge cases:**
- [how each is handled]

**Patterns to follow:**
- [specific files/classes to mirror]

**Acceptance criteria:**
- [ ] [verifiable pass/fail condition with how to check it]
- [ ] ...

Is this correct? Anything to add or change?
```

Do NOT proceed until the developer confirms. If they correct something, update the summary and
confirm again.

## Phase 4: Produce and save the plan

Once the developer confirms, do the following:

### 4a. Sync the issue

The issue exists (Phase 0 required its branch). If the agreed scope or acceptance criteria are
sharper than the issue body, update its description with `mcp__kendo__update-issue-tool`,
written **against** the Feature template in
[`.claude/skills/kendo/references/issue-templates.md`](../kendo/references/issue-templates.md)
(User Story / Context / Acceptance Criteria / Scope / Testing). Don't improvise a structure.
Leave the issue alone when it already matches.

### 4b. Write DECISIONS.md

The plan folder is `docs/issues/<branch-name>/`, the exact current branch name from Phase 0 (e.g.
`docs/issues/BD-0012-server-endpoint-registry/`). Don't mint a different slug: every reader finds
the folder through [`.claude/references/plan-directory.md`](../../references/plan-directory.md),
which looks for the branch name.

Write decisions to `docs/issues/<branch-name>/DECISIONS.md` **as they are made** during planning,
not after; rejected proposals and their reasoning are valuable context.

Use the format and rules in [`references/decisions-template.md`](references/decisions-template.md).

### 4c. Write PLAN.md

Save the plan to `docs/issues/<branch-name>/PLAN.md` using the structure in
[`references/plan-template.md`](references/plan-template.md). The template is the contract:
downstream agents and skills (`plan-reviewer`, `surface-reviewer`, `/task-writer`, `/next`,
`precedent-reviewer`) parse the section names, so don't rename or omit them.

### 4d. Template completeness check (mandatory, fail-closed)

Before spawning the reviewers, verify every section of the saved PLAN.md is substantive. Mark each row **OK** if the bullet rule is met **or** if the section explicitly declares `N/A: <one-line reason>`. Otherwise **THIN**; re-work before continuing.

Load [`references/quality-gates.md`](references/quality-gates.md) for the rationale, the N/A carve-out, and sycophancy guards.

| Section | OK requires |
|---|---|
| Goal | one sentence; describes the outcome for a user of Bend (a client or server author, or a peer on the wire), not implementation |
| Key Design Decisions | ≥ 1 row per non-trivial new class; every choice cites a concrete codebase or `CLAUDE.md` reference |
| Scope (In) | explicit list, not "the feature" |
| Scope (Out) | ≥ 1 explicit non-goal |
| Sweep Inventory | present only when the plan adds a cross-cutting rule; then every such rule has its sibling-site table (from the Phase 1.4 grep), zero unmarked rows. No rule added → no heading |
| Security & Cost Surface | seven rows, each a prose answer to the row's questions ending in a Proof line, or `N/A: <reason>`; carried forward from Phase 1.6 |
| Approach | enumerates files in implementation order, module by module |
| Acceptance Criteria | ≥ 3 verifiable rows with a Verification column filled in |
| Reuse and patterns | ≥ 1 file path marked call or mirror, **or** an explicit "no reuse: building from scratch because X" line; not "follow project conventions" |
| Testing Strategy | per-PR test table with named test classes, `method_condition_expected` test names, behavioural descriptions, and a **red case** per test ("fails when ___"; an unstatable red case means the test is decoration); every new guard or limit names the test that feeds it the input it must reject; every new test class names the Gradle command that runs it (`./gradlew :bend-<module>:test --tests '<fqcn>' --rerun`). No restated standing rules (coverage, JaCoCo, test framework); `CLAUDE.md` carries those |
| Edge Cases | ≥ 3 cases drawn from the Phase 1.5 ✓ Edge Cases evidence |
| Risks | ≥ 1 specific risk; not "the implementation may have bugs" |
| Documentation Sync | every row (`protocol-v1.md`, `client-api` Javadoc, `CLAUDE.md`) marked Update or `N/A: <reason>` |
| DECISIONS.md entries | each 5–10 non-blank lines, heading included, in the Context / Decision / Rejected shape of [`decisions-template.md`](references/decisions-template.md); a longer entry is split or trimmed |
| Hedge parentheticals | Zero `(or whatever)` / `(TBD)` / `(or X)` hedges on load-bearing claims. A hedge papers over a decision you owe: decide it and cite it (Phase 1.4), or escalate via `AskUserQuestion`. |

When every row is OK, proceed to Phase 5.

## Phase 5: Self-gate and hand off

**Before showing the plan to the developer**, spawn **two reviewers in parallel** against the saved PLAN.md:

- `plan-reviewer`: scores 1-10 against codebase conventions (packet and value class shape, exception policy, the `ByteBuf` boundary, module dependency direction, test naming, module shape). Owns the convention bar.
- `surface-reviewer`: scores 1-10 against the seven question-shaped rows of the Security & Cost Surface section. Owns the hostile-input / lifecycle / threading / resource / spec-conformance bar.

**Spawn both in a single message with two `Agent()` calls** so they run concurrently. Do not issue them sequentially; if parallel spawning is unavailable in this environment, stop and say so instead. The reviewers are designed to run on a static snapshot of PLAN.md at the same moment; sequencing them lets the second see partial edits from the first and undermines the independent-context property that makes the dual-spawn valuable.

Pass `docs/issues/<branch-name>/` as the plan directory and the `<skill dir>` from Phase 1.4 in each prompt: both read this skill's own reference files (`module-shape-lens.md`; `surface-questions.md` and `quality-gates.md`).

Both must score ≥ 7 to proceed. The two reviewers exist because you'll rationalise your own design choices; context-free agents won't. They probe different surfaces (codebase conventions vs cross-cutting robustness gaps), so a clean score on one doesn't substitute for the other.

**Below 7 on either, the plan body changes before anything re-runs.** Each reviewer returns its findings to you in chat and writes nothing. Take every finding and fold it into the section it names (correct the citation, restructure the class, rewrite the row prose, record a reversed call in DECISIONS.md), then re-run that reviewer. The plan carries no `## Review Notes` and no verdict: a downstream reader wants the plan that passed, not the history of how it got there. One plan reached 1225 lines under the old rule, 769 of them ten rounds of appended notes.

**Two failing re-runs, then stop and ask.** If a reviewer is still below 7 after two re-runs, the disagreement is a design call, not a convention slip: on that same plan, rounds three to five were reviewers finding new faults in the fixes for round two. Put the open finding to the developer with `AskUserQuestion`, recommending an option, and continue only on their answer.

Then present the plan + both reviewer reports to the developer and ask: "Does this look good?". When approved, update PLAN.md `Status: Approved`.

### Handoff

Plan is approved. Stop here; the next step is a separate skill, owned by the developer (or by Claude in continuation): `/task-writer` turns PLAN.md into `docs/issues/<branch-name>/TASKS.md`, then `/next` works through it one task at a time.

Do not invoke `/task-writer` or `/next` from inside this skill. Each downstream skill owns its own quality gate; chaining them here would recreate the mega-skill we just trimmed away.

## Anti-patterns to avoid

See [`references/anti-patterns.md`](references/anti-patterns.md): lessons-from-pain captured outside SKILL.md so they're loaded once on demand, not every invocation.
