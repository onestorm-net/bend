---
name: next
description: |
  Continue working through TASKS.md: find the next uncompleted task, execute it with TDD flow,
  and mark it done. Use this skill whenever the user wants to continue implementation work, pick up
  where they left off, resume after /clear, or work through a task list. Trigger on: "next task",
  "continue working", "keep going", "what's next", "pick up where I left off", "resume work",
  "next step", or any variant of wanting to progress through planned tasks.
---

# Next Task

Continue working through a TASKS.md file: locate it, find the next uncompleted task, execute it
following TDD flow, and mark it done with learnings.

> **No TASKS.md, just PLAN.md + DECISIONS.md?** Run `/task-writer` first. Every planned issue gets
> a TASKS.md, even a small one; this skill only executes tasks, it doesn't plan them.

> **Review runs once per branch, not per task.** Do not spawn reviewer agents after each task;
> per-task reviews burn tokens for near-zero signal (first-pass reviews came back clean the vast
> majority of the time). The gate for a task is **verification** (its targeted test run). Reviewers
> run once via `/review-branch` before `/pr`.

## Step 1: Locate TASKS.md

The planning folder is `docs/issues/<branch-name>/`, where the folder name is the exact current
branch name (e.g. `docs/issues/BD-0012-namespaced-id/`). Resolve it with
[`.claude/references/plan-directory.md`](../../references/plan-directory.md), then look for
`TASKS.md` inside it.

If the folder holds `BUG.md` instead of `PLAN.md`, this is a bug branch; point the user at
`/fix-bug` instead. If the folder has `PLAN.md` but no `TASKS.md`, point the user at
`/task-writer`. If no folder exists, ask the user where their task file is.

## Step 2: Show progress and find next task

Read the TASKS.md file and report a quick progress summary:

```
Progress: 1/3 tasks complete, starting 1.2
```

Find the first task not marked with `[x]`. If all tasks are complete, summarize the work done and
suggest `/review-branch` as the next step.

## Step 3: Recover context

This is important when resuming after a `/clear` or at the start of a new session; the Context
block in each task is specifically designed for this.

Read the task's **Context** section to understand:

- **Why**: the problem this task solves
- **Architecture**: how it fits in, which patterns to follow
- **Key refs**: file paths and line numbers to read as entry points
- **Watch out**: edge cases and gotchas to keep in mind

Read the files mentioned in **Key refs** and **Touches** to build working understanding of the
code you'll be modifying. Also glance at the previous completed tasks (their **Learnings** and
**Key Changes**) to understand what's already been done in this feature.

If the task Context references decisions (e.g., "see D2"), read `DECISIONS.md` in the same
planning folder to understand the reasoning behind the chosen approach, especially which
alternatives were rejected and why.

If the task touches the wire format, a packet, or a disconnect condition, read the matching
section of `protocol/protocol-v1.md` too. Spec and code change together.

## Step 4: Execute the task

### Follow TDD ordering

Tasks use `[RED]` and `[GREEN]` tags in their action items to indicate TDD flow:

- **[RED]**: write tests first (they should fail initially)
- **[GREEN]**: implement code to make the tests pass

Always execute [RED] items before [GREEN] items. Run the targeted test after the RED step and
confirm it fails for the expected reason (an assertion or a missing class, not a typo).

### Match the module's test conventions

Before writing any test code, read 1-2 existing tests in the module you're touching (in
`protocol/src/test/java` if the module has none yet, e.g.
`net/onestorm/bend/protocol/endpoint/EndpointTest.java`) and match them:

- JUnit Jupiter + AssertJ (`assertThat`, `assertThatThrownBy(...).isInstanceOf(...)`)
- Test methods named `method_condition_expected` (e.g. `readString_overMaxLength_throws`), never "should..."
- One test class per production class, same package under `src/test/java`

### Use task scope as guardrails

The **Scope**, **Touches**, and **Action items** define what this task covers. Stay within those
boundaries; don't start work that belongs to a later task. If you discover something that needs
doing but isn't in the current task, note it for later rather than doing it now.

Keep Bend's design rules from `CLAUDE.md` in view while implementing: no `ByteBuf` in
`client-api`, `ProtocolException` for wire input vs. `IllegalArgumentException` for caller
misuse, built-in packets never sent through the public endpoint send path, and no defensive check
for a case the current code can't produce.

## Step 5: Verify before completing

After implementation, work through the task's **"Verify before complete"** section and run every
check you can. Don't block on checks that need the user's judgement; flag them.

Gates:

| Situation | Command |
|---|---|
| Task touches one module | `./gradlew :bend-<module>:test --tests '<fqcn>[.method]' --rerun` |
| Task touches more than one module, or is the final verification task | `./gradlew build` |

Use the Gradle project path (`:bend-protocol`, `:bend-client-api`, `:bend-client-netty`,
`:bend-server-netty`), never the folder name, and always `./gradlew`, never `gradle`. `--rerun`
stops an UP-TO-DATE task from reporting a pass without running anything. javac is the type check;
a run that compiles has passed it. Bend has no CI, git hooks, linter, or formatter, so nothing
else runs these for you.

For the final verification task, also do the JaCoCo gap check: open
`<module>/build/reports/jacoco/test/jacocoTestReport.xml` for each touched module, find each
touched class's `<sourcefile>`, and list `<line>` elements with `mi` greater than 0. For each
uncovered line, ask "is this reachable at all" before writing a test. Reachable → write a real
test. Unreachable → propose deleting it. Never write a contrived reflection test to turn it green.

**Verification is the per-task gate.** Reviewer agents don't run per task; they run once against
the full branch via `/review-branch` before `/pr`.

## Step 6: Mark complete with metadata

Update the TASKS.md file:

1. Change `- [ ] **N.1**` to `- [x] **N.1**` on the task
2. Add completion metadata below the task's existing content:

```markdown
- [x] **N.1** Implement feature X
    - [existing task content...]
    - **Completed:** YYYY-MM-DD
    - **Learnings:**
        - Key insight discovered during implementation
        - Gotcha or edge case encountered
    - **Key Changes:**
        - Created `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`
        - Modified `protocol/src/main/java/net/onestorm/bend/protocol/util/ProtocolUtil.java`
    - **Notes:** Any important context for future tasks
```

Write meaningful learnings: things that weren't obvious from the task description, edge cases
you hit, patterns you discovered. These help future tasks and context recovery.

## Step 7: Report and suggest commit

Summarize what was done, then suggest committing the work as a natural checkpoint:

```
Done: 1.1 (NamespacedId with 11 tests, :bend-client-api tests green)

Suggest committing before continuing. Want me to /commit?
```

If the user wants to continue, loop back to Step 2 for the next task.
