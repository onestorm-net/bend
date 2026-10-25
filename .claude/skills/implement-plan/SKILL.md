---
name: implement-plan
description: |
  Implement a small feature straight from PLAN.md and DECISIONS.md, without a TASKS.md breakdown.
  Use when the plan's Approach is already a good enough roadmap: about 5 or fewer steps, inside one
  module. Loads the plan, works through the Approach with TDD, runs the targeted tests, the full
  build and the JaCoCo gap check, then records learnings in IMPLEMENTATION.md so a later session
  can resume. Trigger on: "implement the plan", "implement plan", "build this plan", "execute the
  plan", "/implement-plan", "just do the plan", or whenever the developer wants to build a planned
  feature without writing TASKS.md first. Prefer /next when TASKS.md already exists.
---

# Implement Plan

Execute an approved plan end-to-end without a TASKS.md in between. Built for small plans, where
PLAN.md's Approach already gives enough structure that a task breakdown would only restate it.

This skill is the sibling of `/next`: same context recovery, TDD ordering and verification, but it
walks the plan's Approach instead of a checklist.

Bend facts this skill relies on: planning folders are `docs/issues/<branch-name>/`; Gradle is
always `./gradlew` with project paths `:bend-protocol`, `:bend-client-api`, `:bend-client-netty`,
`:bend-server-netty`; there is no CI, no git hooks and no linter or formatter, so the local build
is the only gate.

## When to use this vs. /next

| Situation | Use |
|---|---|
| `TASKS.md` already exists in the plan folder | `/next` (don't re-plan; execute the breakdown) |
| Approach has about 5 or fewer steps, all in one module (docs-only changes count as none) | `/implement-plan` |
| Several modules, a wire change spread across `protocol` and an implementation module, or several natural commit points | `/task-writer`, then `/next` |
| No `PLAN.md` | Stop. Run `/plan-feature` first. |
| `BUG.md` instead of `PLAN.md` | Stop. This is a bug branch: `/fix-bug`. |

When unsure, ask the developer; `/plan-feature`'s handoff names both options.

## Step 1: Locate the plan folder

Resolve `docs/issues/<branch-name>/` with
[`.claude/references/plan-directory.md`](/.claude/references/plan-directory.md) and look for
`PLAN.md` inside it. If it isn't found, ask the developer where the plan lives; don't guess.

If a `TASKS.md` sits next to `PLAN.md`, **stop and tell the developer**: they probably want
`/next`. Continue only on their confirmation.

If `PLAN.md`'s `**Status:**` isn't `Approved`, say so and ask before implementing.

## Step 2: Load the full context

Read every plan document that exists in the folder:

| File | Why it matters |
|---|---|
| `PLAN.md` | The contract: goal, scope (in/out), approach, acceptance criteria, testing strategy |
| `DECISIONS.md` | The reasoning: rejected alternatives, and why the plan looks the way it does |
| `IMPLEMENTATION.md` | Earlier progress; only present if a previous session did part of the work |

If `IMPLEMENTATION.md` exists, read it first and report what's already done. Resume rather than
restart.

Then read root `CLAUDE.md`, the `CLAUDE.md` of every module the plan touches, and the files named
in the Approach and in **Reuse and patterns**, so you know the code as it is, not only the paths.
If the plan touches the wire format, a packet or a disconnect condition, read the matching section
of `protocol/protocol-v1.md` as well: spec and code change together.

## Step 3: Show your understanding

Before writing any code, send the developer a short orientation, under 8 lines:

- The goal in one sentence
- The Approach steps as a numbered list, paraphrased
- Anything in the plan that looks ambiguous or stale against the current code

If anything looks wrong, **ask before proceeding**. If it all looks fine, carry on without waiting
for a "yes"; the developer can interrupt.

## Step 4: Match the module's test conventions

Bend has no testing skill. Before writing any test, read 1-2 existing tests in the module you're
touching (or in `protocol/src/test/java` if the module has none yet, e.g.
`net/onestorm/bend/protocol/endpoint/EndpointTest.java`) and match them: JUnit Jupiter and AssertJ
(`assertThat`, `assertThatThrownBy(...).isInstanceOf(...)`), methods named
`method_condition_expected`, one test class per production class in the mirrored package under
`src/test/java`.

## Step 5: Execute the Approach

Walk the Approach steps in order. For each one:

- **Tests first.** If the step adds or changes behaviour, write the failing test before the code,
  using the plan's Testing Strategy rows as the spec. Run it and confirm it fails for the expected
  reason (an assertion or a missing class, not a typo).
- **Stay inside Scope (In).** Something that needs doing but is out of scope, or unmentioned, gets
  noted for later, not done now.
- **Honour the decisions.** Don't re-litigate DECISIONS.md mid-implementation. If one no longer
  fits, put it to the developer as a question; never deviate silently.
- **Keep Bend's rules in view:** no Netty types in an API module, `ProtocolException` for wire
  input and `IllegalArgumentException` for caller misuse, built-in packets never through the public
  send path, no defensive check for a case the code can't produce.
- **Pause at commit points.** Where the Approach has a natural break, stop so the developer can
  `/commit`. `/commit` keeps PLAN.md and DECISIONS.md in step with the diff.

## Step 6: Verify

Run, in order:

1. The targeted tests for each new or changed test class:
   `./gradlew :bend-<module>:test --tests '<fully.qualified.TestClass>' --rerun`. `--rerun` stops an
   UP-TO-DATE task from reporting a pass without running anything.
2. Every acceptance criterion in PLAN.md whose Verification column you can run yourself.
3. `./gradlew build`.
4. The JaCoCo gap check: in `<module>/build/reports/jacoco/test/jacocoTestReport.xml`, find each
   touched class's `<sourcefile>` and list `<line>` elements with `mi` (or `mb`) greater than 0.
   For each, ask "is this reachable at all". Reachable → a real test. Unreachable → propose
   deleting it. Never a contrived reflection test.

Don't block on checks that need the developer's judgement; flag them.

## Step 7: Capture learnings in IMPLEMENTATION.md

Create or append `docs/issues/<branch-name>/IMPLEMENTATION.md`. It is what lets the next session
resume without re-reading the diff, and it takes the place of TASKS.md's completion metadata.

```markdown
# Implementation Log

## YYYY-MM-DD: <one-line summary of this session's work>

**Key Changes:**
- Created `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`
- Modified `protocol/src/main/java/net/onestorm/bend/protocol/util/ProtocolUtil.java`: what and why

**Learnings:**
- Something non-obvious discovered while implementing
- An edge case that wasn't in the plan

**Deviations from plan:**
- (Write the section even when empty, so a reader knows nothing was skipped)

**Follow-ups noted but not done:**
- Out-of-scope work worth an issue
```

Learnings are things PLAN.md didn't say. "It worked" is not one. If `IMPLEMENTATION.md` already
existed, append a new dated section rather than overwriting.

## Step 8: Report and hand off

Summarise what was built and the verification results, then suggest the next steps:

```
Implemented BD-NNNN: <one-sentence summary>
:bend-<module> tests and ./gradlew build green; JaCoCo: no unexplained gaps. IMPLEMENTATION.md updated.

Suggest /commit, then /review-branch before /pr.
```

Don't invoke `/commit`, `/review-branch` or `/pr` from here; each owns its own step, and review
runs once per branch.

If the developer wants to keep iterating, go back to the relevant step rather than restarting.
