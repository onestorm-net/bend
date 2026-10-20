# Quality gate: Phase 4 (coverage)

`/task-writer` runs one fail-closed gate before TASKS.md is shown to the
developer. The checklist itself lives in `verification.md`; this document
carries the rationale, sycophancy guards, and operating notes. Load it when
you reach the gate.

## Why a gate at all

The path of least resistance ("this looks crisp, let me hand it over and we
can iterate") is the most expensive failure mode in task breakdown. A
complete-looking breakdown can still miss an acceptance criterion, a
disconnect condition, or a spec change, and that miss travels all the way to
the PR.

There is deliberately **no right-size gate** inside this skill. Whether a plan
needs a TASKS.md at all is decided before it runs: a small plan (about 5 or
fewer Approach steps, one module) goes to `/implement-plan` instead. Once this
skill runs, a small plan produces a short TASKS.md, not no TASKS.md.

## Phase 4: coverage gate

**The rule:** walk the self-check in `verification.md` and fix every unchecked
row before handing off. Fail-closed, and self-administered: no agent audits
this afterwards, so honesty here is the whole gate.

### Sycophancy guards specific to the output

You are checking your own work, which is exactly the condition under which
"close enough" wins. Three guards:

- **"Most of the ACs are covered" is not a passing answer.** Classify each AC
  as COVERED / IMPLICIT / MISSING. **Implicit is a yellow flag**: the task
  probably covers the AC but doesn't say so. The fix is usually adding an
  `**Acceptance Criteria:**` line, not creating a new task.
- **Three IMPLICIT verdicts is fine to ship. One MISSING is not**, least of
  all when it's rationalised as "the developer can figure it out."
- **Write the counts down, not an adjective.** "6/6 acceptance criteria,
  9/9 planned files" is checkable by the next reader. "Coverage looks good"
  is what this gate produces when it isn't really being run.

### What to verify

Read PLAN.md and TASKS.md side by side:

1. Every acceptance criterion maps to at least one task's
   `**Acceptance Criteria:**` field
2. Every PLAN.md "in scope" item is covered by a task
3. No task introduces work outside the plan's scope (scope creep)
4. Every planned file appears in some task's `**Touches:**`
5. Every `protocol-v1.md` change sits in the same task as the code that
   implements it
6. Critical edge cases from PLAN.md (hostile length prefixes, half-open
   peers, unregister with messages in flight) are addressed by task action
   items or test descriptions

Don't assess task quality, TDD ordering, or whether the breakdown reads well.
Nothing downstream does either: TASKS.md is scaffolding for execution, not a
reviewed artifact. The pre-PR reviewers grade the code, not the plan for
writing it.

### When you find scope creep

A task that doesn't trace back to the plan has two valid responses:

1. **Plan was incomplete**: the task is real work the plan missed. Update
   PLAN.md to include it, with the developer's confirmation.
2. **Task is genuine creep**: it's doing work the plan didn't ask for. Remove
   it from TASKS.md.

Don't paper over it by adding the task to the plan retroactively without that
confirmation; it erodes the plan's role as the contract.

### Audit trail

Record the coverage counts in a `## Review Notes` section at the bottom of
TASKS.md (template in `SKILL.md` Phase 4), so a future reader sees the coverage
without re-deriving it. Replace it on re-runs rather than appending a second
one.
