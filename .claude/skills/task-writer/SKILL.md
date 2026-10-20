---
name: task-writer
description: |
  TDD task breakdown and TASKS.md management. Use when planning a new feature, breaking down work
  into implementation tasks, creating or updating TASKS.md, estimating effort, or organizing
  multi-step work into phases. Also use when translating a plan document (docs/issues/<branch>/PLAN.md)
  into TASKS.md; includes a systematic extraction checklist to ensure nothing is missed. Trigger on:
  feature breakdown, task planning, "write tasks for", "create TASKS.md", plan-to-tasks conversion,
  implementation ordering, or when the user mentions wanting to organize implementation steps
  for a feature.
---

# Task Writer

Translate an approved PLAN.md into a TDD task breakdown (`TASKS.md`) that
`/next` can execute one task at a time. Lighter than `/plan-feature` (no
interrogation, no design), but it owns the per-task contract that `/next`
reads.

**Every planned issue gets a TASKS.md, even a small one.** `/next` is the only
execution path for planned work in Bend, and it needs a task list to execute.
A small plan just produces a short TASKS.md (often a single implementation task
plus the final verification task); see
[`references/task-sizing.md`](/.claude/skills/task-writer/references/task-sizing.md).

## Core philosophy

Bundle related work (value types, codec, handler wiring, tests) into one
logical unit. Split by integration boundaries, not by files. Default to
**coarser-grained tasks**: one coherent module end-to-end ("protocol: packet
class + codec registration + spec section + tests") is *one* task, not three.
Splitting at intra-module boundaries creates handoffs without reducing risk;
the model holds a full module coherently and the reviewer reads commits, not
tasks.

For sizing rules, approval gates, and the decision matrix that drives
splits, load [`references/task-sizing.md`](/.claude/skills/task-writer/references/task-sizing.md).

Rationale for the Phase 4 coverage gate (sycophancy guards, scope-creep
handling) lives in
[`references/quality-gates.md`](/.claude/skills/task-writer/references/quality-gates.md).

## Phase 1: Build extraction inventory

Read PLAN.md (and DECISIONS.md if present) and extract every concrete artefact
into a category checklist. The full category list (packet/codec changes, spec
changes, public API types and Javadoc, pipeline wiring, disconnect conditions
and `ProtocolException` paths, threading/lifecycle, test plans) and the "what
fits where" table live in
[`references/extraction-workflow.md`](/.claude/skills/task-writer/references/extraction-workflow.md).
Load it the moment you start the inventory.

Output the inventory verbatim to the developer with every line filled in.
**Every PLAN.md item must land in a category.** If a PLAN.md item doesn't
fit, the plan has a gap; surface it before continuing. Don't shoehorn
ambiguous items into a code category to keep the inventory looking
complete; that's how scope creep ships.

## Phase 2: Map inventory to tasks

Group inventory items by **integration boundary**, not by file. Bend's
boundaries run in dependency order:

| Inventory category | Maps to |
|---|---|
| Packets + codecs + `protocol-v1.md` changes + protocol tests | `protocol` task |
| Public interfaces + value types + Javadoc + unit tests | `client-api` task |
| Pipeline wiring + handlers + disconnect paths + threading | `client-netty` / `server-netty` task |
| Client to server behaviour across the wire | Round-trip test task |
| Disconnect conditions / hostile input | Task Context "Watch out" + `[RED]` tests |
| Spec text the code must match | Task Context "Architecture" |
| Plan's verification steps | Final verification task |

Spec changes to `protocol-v1.md` travel in the same task as the code that
implements them. Never split "update the spec" into its own task; spec and
implementation drifting apart between tasks is exactly the drift the
2026-09-16 audit had to clean up.

Write the tasks using the literal format in
[`references/tasks-template.md`](/.claude/skills/task-writer/references/tasks-template.md). The
template is the contract: `/next` parses the section names, so don't rename
or omit them.

For sizing decisions (when to split, when to bundle), load
[`references/task-sizing.md`](/.claude/skills/task-writer/references/task-sizing.md). For
pre-completion verification specifics (what to put in the "Verify before
complete" block), load
[`references/verification.md`](/.claude/skills/task-writer/references/verification.md).

## Phase 3: Verify complete coverage

Cross-check before finalising:

```markdown
## Coverage Check

- [ ] Every "Files to modify" appears in at least one task's Touches
- [ ] Every "Files to create" appears in at least one task's Touches
- [ ] Every disconnect condition / error path from the plan is in a task's "Watch out" or action items
- [ ] Every spec change is in the same task as the code that implements it
- [ ] Every verification step from the plan is in the final verification task
- [ ] Every PLAN.md acceptance criterion is mapped to at least one task's `**Acceptance Criteria:**` line
```

If any row is unchecked, you're not done. Don't punt to "the reviewer will
catch it"; a clean self-check makes later review re-confirmation, not
discovery. The full self-check (structure, context completeness,
verification quality, AC coverage, plan coverage) lives in
[`references/verification.md`](/.claude/skills/task-writer/references/verification.md).

## Phase 4: Coverage self-gate (mandatory, fail-closed)

**Before showing TASKS.md to the developer**, walk the coverage checklist in
[`references/verification.md`](/.claude/skills/task-writer/references/verification.md) § *Self-check before
finalising TASKS.md*. Every row must pass. A missed acceptance criterion is the
one failure that propagates all the way to the PR, so this gate is fail-closed:
fix gaps and re-walk rather than noting them and moving on.

Then record the result at the bottom of TASKS.md so a later reader (or a
future you after `/clear`) can see the coverage without re-deriving it:

```markdown
## Review Notes

**Reviewed:** <YYYY-MM-DD>

### Coverage
- **Acceptance criteria:** N/N covered
- **Planned files:** N/N appear in some task's `**Touches:**`

### Gaps
- <each AC or planned file with no task, and what you did about it>
- *(None)* if the checklist passed clean

### Scope Drift
- <each task doing work the plan doesn't describe>
- *(None)*
```

**On scope drift, two valid responses:** update PLAN.md to include the missed
work (with the developer's confirmation), or remove the task. Don't paper over
the gap by silently editing the plan.

> **Why no reviewer agent here.** A dedicated alignment-reviewer agent used to
> run this gate. Across 26 recorded runs it never once scored below 8: it
> filed real coverage gaps 38% of the time, but always alongside a passing
> score, so it never actually gated anything. Doing the self-check in
> `verification.md` honestly is the gate.

## Phase 5: Hand off

Tasks are written and the Phase 4 coverage gate passed. Stop here;
implementation is owned by `/next`, which executes one task at a time with
the TDD loop and uses verification (targeted `./gradlew ... test --rerun`,
javac via the build) as the per-task gate.

Do not invoke `/next` or any reviewer from inside this skill. Each downstream
skill owns its own quality gate; review runs once per branch via
`/review-branch` before `/pr`.

## Output

Write `TASKS.md` into the branch's planning folder, `docs/issues/<branch-name>/`, alongside
`PLAN.md` and `DECISIONS.md`. The folder name is the exact branch name (e.g.
`docs/issues/BD-0012-namespaced-id/`); resolve it with
[`.claude/references/plan-directory.md`](/.claude/references/plan-directory.md).

**If that finds nothing** (a plan written ad hoc, with no tracked issue or branch yet): create a
Kendo issue in project 1 via `/kendo` (follow its issue template rather than improvising one),
then create the branch `BD-NNNN-<kebab-slug>` with `/newbranch` and use
`docs/issues/BD-NNNN-<kebab-slug>/`. Create the directory if it doesn't exist yet.

**TASKS.md header** includes the issue key and links to the plan and decisions:

```markdown
# BD-NNNN: Short Description

> Plan: [PLAN.md](/docs/issues/BD-NNNN-<slug>/PLAN.md) | Decisions: [DECISIONS.md](/docs/issues/BD-NNNN-<slug>/DECISIONS.md)
```

## Worked example

A `client-api` feature (the `NamespacedId` value type) showing the task format
with TDD ordering, exact Gradle commands, the final verification task, and
completion metadata lives in
[`references/example-client-api-feature.md`](/.claude/skills/task-writer/references/example-client-api-feature.md).
Reference it when you need a concrete output to model your TASKS.md against.
