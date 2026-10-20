# TASKS.md template

Save the breakdown as `TASKS.md` in the branch's planning folder (see this skill's own Output
section), e.g. `docs/issues/BD-0012-namespaced-id/TASKS.md`. Use this exact structure: `/next`
parses it. Renaming or omitting sections breaks downstream skills silently.

## Task format

```markdown
### Phase N: Feature Name (X tasks)

**Goal:** One sentence describing what this phase delivers.

**Phase Context:** (only if needed for phase-level decisions)

- Why NOT [alternative]: [Key decision that's not obvious from code]

- [ ] **N.1** Implement [feature/module]
    - **Context:** (REQUIRED, enables standalone task pickup)
        - **Why:** [Problem this solves, what triggered this task. Reference decisions: "see D2"]
        - **Architecture:** [How it fits in, which pattern to follow, e.g. "Follows the validation pattern of Endpoint's constructor"]
        - **Key refs:** [Specific file:line references to understand entry points and integration]
        - **Watch out:** [Edge cases, gotchas, things that aren't obvious]
    - **Scope:** Brief description of what's included
    - **Acceptance Criteria:** AC #N, #N (partial: what this task covers vs later tasks). List the PLAN.md acceptance criteria numbers this task addresses. Use "(partial)" when the task only implements part of an AC. Omit for scaffolding tasks with no direct ACs.
    - **Touches:** Key files (not exhaustive, just main ones)
    - **Action items:**
        - [RED] Write tests for [specific behavior 1]
        - [RED] Write tests for [specific behavior 2]
        - [GREEN] Implement [class] to make tests pass
        - [GREEN] Integrate into [existing code]
    - **Verify before complete:**
        - [ ] `./gradlew :bend-<module>:test --tests '<fqcn>' --rerun` passes
        - [ ] [Watch out item 1]: addressed by [how/test name]
        - [ ] [Watch out item 2]: addressed by [how/test name]
        - [ ] Integration: [specific check]
    - **Success:** [One-line summary of done state]

- [ ] **N.2** Final verification
    - **Verify:**
        - [ ] `./gradlew build` passes
        - [ ] JaCoCo gap check: `<module>/build/reports/jacoco/test/jacocoTestReport.xml` shows no uncovered lines (`mi > 0`) in [touched classes], or each remaining one is explained
    - **Success:** Full build green, no unexplained coverage gaps in touched classes
```

## Required sections per task

| Section | Required when | Notes |
|---|---|---|
| **Context** | Every implementation task | Enables pickup after `/clear`. Don't pad it; write the non-obvious. |
| **Acceptance Criteria** | Every implementation task | List AC numbers from PLAN.md. Use "(partial)" when only part of an AC is covered. Omit for scaffolding tasks. |
| **Touches** | Every implementation task | Indicative file paths, not exhaustive. |
| **Action items** | Every implementation task | `[RED]` tests **before** `[GREEN]` implementation. No exceptions. |
| **Verify before complete** | Every implementation task | The exact targeted Gradle command, one row per "Watch out" item, plus integration checks. |
| **Success** | Every task | One line. The done-state predicate. |

## Completion metadata

When a task is completed, enrich it with what was learned. This is valuable
for future tasks and context recovery:

```markdown
- [x] **N.1** Implement [feature/module]
    ...existing content...
    - **Completed:** YYYY-MM-DD
    - **Learnings:**
        - [What was discovered during implementation that wasn't obvious]
        - [Gotchas that future tasks should know about]
    - **Key Changes:**
        - Modified `protocol/src/main/java/net/onestorm/bend/protocol/util/ProtocolUtil.java`: [what changed and why]
        - Created `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`: [purpose]
```

## TDD ordering: non-negotiable

Action items MUST list `[RED]` test-writing steps **before** `[GREEN]`
implementation steps. The order encodes TDD discipline; reversing it
encourages "implement, then write tests that pass" anti-patterns.

```
Task: Bound array length prefixes before allocating
  ↓
  Action items (in this order):
    [RED]   1. readVarIntArray_lengthExceedsAvailableBytes_throwsWithoutAllocating
    [RED]   2. readVarIntArray_negativePrefixedLength_throws
    [RED]   3. availableEndpoints_endpointCountExceedsAvailableBytes_throwsWithoutAllocating
    [GREEN] 4. Add ProtocolUtil.validateArrayLength(buffer, length)
    [GREEN] 5. Call it from readVarIntArray and readEndpointArray before allocating
  ↓
  ./gradlew :bend-protocol:test --tests 'net.onestorm.bend.protocol.util.ProtocolUtilTest' --rerun
  ↓
Task complete
```

Tests follow the module's existing conventions: JUnit Jupiter + AssertJ
(`assertThat`, `assertThatThrownBy`), test methods named
`method_condition_expected`, one test class per production class in the
mirrored package under `src/test/java`. Read 1-2 existing tests in the module
(e.g. `protocol/src/test/java/net/onestorm/bend/protocol/endpoint/EndpointTest.java`)
before writing new ones. No "Testing:" footer: tests **are** the first action
items.

## Capturing non-code requirements

Plans contain specifications that aren't "write code for file X". Capture
them inside the task structure:

### Spec text → Architecture

```markdown
- **Context:**
    - **Architecture:** protocol-v1.md § Frame: a frame's `Length` MUST NOT be 0
      (every frame carries at least a 1-byte packet id). Leading `0x00` bytes are
      padding and are skipped before the length is read; more than 16 in a row
      MUST disconnect.
```

### Disconnect conditions and hostile input → Watch out + `[RED]` tests

```markdown
- **Watch out:**
    - Array length prefix claims more elements than bytes remain → ProtocolException, before any allocation
    - Negative length prefix (5-byte VarInt) → ProtocolException
    - OutOfMemoryError is an Error, not caught by the decoder's catch (Exception e), so the check must come first
- **Action items:**
    - [RED] Write tests for oversized, negative, and exact-fit length prefixes
```

### Verification steps → final verification task

```markdown
- [ ] **N.3** Final verification
    - **Verify:**
        - [ ] `./gradlew build` passes
        - [ ] JaCoCo gap check: `protocol/build/reports/jacoco/test/jacocoTestReport.xml` shows `mi="0"` on every line of `ProtocolUtil` and `AvailableEndpointsPacket`
        - [ ] Any line that stays uncovered: traced to "is this reachable at all", then tested or deleted (never a reflection test)
    - **Success:** Full build green, touched classes fully covered or each gap explained
```

## Conventions

- `- [ ]` for incomplete, `- [x]` for complete
- Number as `Phase.Task` (e.g. `3.2`)
- File paths in **Touches** are indicative, not exhaustive; listing every file is noise
- Keep descriptions concise; the Context block is for the non-obvious, not for re-hydrating basic plan context
- The Context block is written primarily for code reviewers and `/clear`-recovery, not for the current Claude session

## What goes IN a single task: bundle these together

- Value types / interfaces needed for the feature
- The implementation (packet, codec, handler)
- Tests for it (written first, TDD)
- The matching `protocol-v1.md` change, if any
- Javadoc for anything public in `client-api`
- Integration into existing code (registration tables, pipeline setup)

For when to **split** instead of bundle, see [`task-sizing.md`](/.claude/skills/task-writer/references/task-sizing.md).
