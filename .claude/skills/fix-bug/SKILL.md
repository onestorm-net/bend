---
name: fix-bug
description: |
  Structured bug-fix workflow: identify the issue, reproduce the defect,
  confirm it isn't already fixed on development, capture the investigation in
  docs/issues/<branch-name>/BUG.md, propose the fix and wait for developer
  approval, implement it, and gate on the bug-fix-verifier agent (with
  runtime-integrity-reviewer alongside) before PR. Use whenever the user wants
  to fix a bug, says "fix bug", "bug fix", "debug this issue", "work on bug
  <key>", "/fix-bug", or is picking up a bug-type issue. Prefer this over
  /plan-feature for bugs: bugs don't have acceptance criteria, they have "does
  the defect still reproduce?". This skill is the bug-side analogue of
  /plan-feature + /task-writer + /next collapsed into one lighter flow.
---

# Fix Bug

End-to-end bug-fix workflow. Lighter than `/plan-feature`: bugs don't need
interrogation, task breakdown, or acceptance criteria. They need a confirmed
reproduction, a root cause, an approved fix, and proof the fix held.

If a bug touches multiple modules with non-trivial design work (e.g. a race
that reveals a missing synchronisation point between the event loop and the
endpoint registry, or a fix that needs a `protocol-v1.md` change), promote it
to `/plan-feature` instead. The extra structure is worth it.

**Path 3b carve-outs.** Mechanical fixes (typo / off-by-one / null-deref with
a stack trace) skip the hypothesis-ranking ceremony in Phase 6 and the
post-mortem prompt in Phase 10. Carve-out details live inline in those phases.

## Phase 0: Parse arguments

The developer may pass an issue key, an issue URL, or nothing.

**Issue key ≠ issue ID.** A key like `BD-0043` does NOT mean the database ID is
43. Never extract the number from a key and use it as an ID.

If no argument, auto-detect from the current branch: branches from
`/prepare-issue` look like `BD-NNNN-<kebab-slug>`. If the branch has no key,
ask which issue this is.

## Phase 1: Fetch the issue

Read the issue from Kendo through the `/kendo` skill (local MCP server
`kendo`, `mcp__kendo__*` tools, project_id `1`). Keep in memory:

- **id**, **key**, **title**
- **type**: must be a bug. If it's a feature or task, stop and point the user
  at `/plan-feature`.
- **description**, **comments**, **attachments**

Note any existing **branch links**: a branch may already exist.

## Phase 2: Branch guard

`<base>` is `development`, Bend's integration branch. Every later phase compares
against `origin/development` (or local `development` when the remote ref is missing).

This skill does **not** create branches. That's `/prepare-issue`'s job (or
`/newbranch` for work without an issue).

| Branch state | Action |
|---|---|
| On `development` or `main` | Stop. Tell the user to run `/prepare-issue <KEY>` or `/newbranch`. |
| Branch name is `BD-NNNN-<slug>` with this issue's key | Proceed. |
| Any other branch | Stop. Ask if this is the right branch; if not, point at `/prepare-issue` or `/newbranch`. |

Hard stop: do not silently create a branch from here. The developer made a
choice at branch-creation time and this skill must not undo it.

## Phase 3: Capture a verifiable repro

First, read the code paths the bug touches, in Bend's chain: frame decoder
(`VarInt28FrameDecoder`) → `PacketDecoder` → packet class → pipeline handler →
endpoint registry/dispatch → public API (`client-api`); outbound the reverse,
through `PacketEncoder` / `VarInt28LengthEncoder`. Read the matching section
of `protocol/protocol-v1.md` too: the spec is the behaviour contract, and a bug
is often spec-vs-implementation drift. You can't pick a repro path without
knowing what the code is supposed to do.

Before picking a path, validate the issue's *Expected behaviour* / *Steps to
reproduce* section against the code and the spec. The validation rules and
the contradiction-surfacing prompt live in
[references/repro-paths.md](/.claude/skills/fix-bug/references/repro-paths.md). Only after the
developer confirms the canonical model do you pick a path.

Every fix needs **something executable that demonstrates the defect**. That's
what Phase 8's verifier checks against.

| Situation | Path |
|---|---|
| Stack trace / error log / clearly-broken line + mechanical fix | **3b** |
| "It's broken somewhere around X": need to pin down behaviour | **3a** |
| Race, timing, half-open peer, event-loop ordering | **3a**, made deterministic (`EmbeddedChannel`, latches, manual time) |
| Report says "it's broken" with no specifics | Ask for more context. Don't guess |

Path details (when each applies, what to write into BUG.md's "Reproduction
Steps", and the "cannot reproduce" stop condition) live in
[references/repro-paths.md](/.claude/skills/fix-bug/references/repro-paths.md). Load that file once
you've picked the path.

## Phase 4: Check if already fixed on development

Before writing a fix, verify the bug still exists on `development`. Your branch may
have diverged and the fix may already have landed.

- **Failing test (3a):** stash your test, `git fetch origin development`, check out
  the file(s) under test from `origin/development` (`git checkout origin/development --
  <path>`), un-stash, run it with `--rerun`. Reset afterwards.
- **Clear diagnosis (3b):** `git log origin/development -- <path>` for recent
  fix-language commits. If line numbers / typos already differ on `development`, read
  the diff and confirm.

If the bug doesn't reproduce on `development`, stop. Tell the user which commit
appears to have fixed it and suggest merging `development` in instead of writing a
duplicate fix.

## Phase 5: Create docs/issues/<branch-name>/BUG.md

The folder is the exact current branch name (`git branch --show-current`), no
suffix stripping: `docs/issues/BD-0043-zero-length-frame/BUG.md`. Features
use the same root with `PLAN.md`; which file exists is what marks a branch as
plan or bug (see `.claude/references/plan-directory.md`). If the folder
already holds a `PLAN.md`, stop and ask: this branch is a feature branch, and
writing a `BUG.md` beside it makes the branch ambiguous to `/review-branch`
and `/pr`.

Write `BUG.md` using the template at
[references/bug-md-template.md](/.claude/skills/fix-bug/references/bug-md-template.md). Fill
**Problem** and **Reproduction Steps** now from your Phase 3 work; leave
**Root Cause**, **Chosen Approach**, **Fix**, and **Verification** empty.
They get filled as you progress. Set Status: `Investigating`.

## Phase 6: Diagnose & propose solution

Explain the bug to the developer and get explicit approval before any
shipping code moves. **No shipping code is edited in this phase**, only
tagged debug instrumentation. The gate exists to catch a wrong diagnosis or a
wrong choice of fix before the diff starts growing.

The mechanics live in
[references/diagnose-and-propose.md](/.claude/skills/fix-bug/references/diagnose-and-propose.md):

- **Generate hypotheses** (3-5 ranked, falsifiable). **Skip for path 3b.**
- **Pin down root cause** and write 2-4 sentences into BUG.md, citing
  `file:line`. Update Status to `Diagnosed`.
- **Debug instrumentation hygiene**: tag temporary output with a
  `[DEBUG-xxxx]` 4-hex prefix so cleanup is one grep.
- **Decompose the fix surface** into settled vs. ambiguous sub-concerns
  before listing candidates.
- **Explain and propose**: bug summary, root cause, candidates with
  trade-offs, recommendation. Cap at 4 candidates.
- **Get acceptance via `AskUserQuestion`**: clickable options, plus the
  rejection-recovery rule for two consecutive dismissals.

**If diagnosis reveals it isn't actually a bug, or a duplicate of an
already-fixed/already-tracked defect**, stop here. Don't propose a fix for a
non-bug. Explain the finding to the developer (cite `file:line`, the
`protocol-v1.md` rule, the CLAUDE.md ruling, or the duplicate issue) and
confirm via `AskUserQuestion`:

> Diagnosis suggests this isn't a bug: `<one-line reason, e.g. "protocol-v1.md
> requires the disconnect, this is the spec working as designed" or
> "duplicate of BD-0031, already fixed on development">`. Abandon this branch?
> - **Yes, abandon**: update Status to `Abandoned`, note why in Notes /
>   Follow-ups, and stop the workflow. Do not proceed to Phase 7 or `/pr`.
> - **No, the diagnosis is wrong / keep investigating**: stay in Phase 6,
>   revise Root Cause with the developer's correction.

For Path 3b this whole phase collapses to a single short message. Don't
ceremonialise a typo into a four-paragraph proposal.

Once a candidate is picked, write a one-liner into BUG.md's **Chosen
Approach**, then proceed to Phase 7.

## Phase 7: Implement the fix

Normal implementation loop, using the **Chosen Approach** from Phase 6.

- **Stick to the chosen approach.** New evidence that contradicts the
  diagnosis or makes the approach unworkable means stop, update Root Cause,
  return to Phase 6 with a fresh proposal, re-ask. Don't quietly switch
  mid-fix.
- **Keep the diff minimal.** A bug fix is not a cleanup pass. Resist renaming
  variables, deleting comments, or refactoring helpers in the same diff.
  Spotted issues go in BUG.md Notes / Follow-ups.
- **Respect the standing rulings.** A fix that adds a guard must add one the
  current code can actually reach, with a real test behind it (CLAUDE.md:
  unreachable "defensive" checks get deleted, never kept with a contrived
  test). Wire-input failures throw `ProtocolException`; caller misuse throws
  `IllegalArgumentException` / `IllegalStateException`. If the fix changes
  observable wire behaviour, `protocol-v1.md` changes in the same diff.
- **Run your repro**: the failing test should now pass.
- **Run the module's suite** for the area you changed:
  `./gradlew :bend-<module>:test --rerun`. javac is the type check, there are
  no hooks or linters. If the fix crosses modules, run `./gradlew build`.
- **Grep out debug instrumentation** before Phase 8 (see
  diagnose-and-propose.md).

Update BUG.md as you go: fill in **Fix**, change Status to `Fixing`, then to
`Verified` only after Phase 8 passes.

## Phase 8: Spawn bug-fix-verifier and runtime-integrity-reviewer

Spawn the `bug-fix-verifier` agent. It has no context from this conversation
and works purely from BUG.md and the diff. It confirms the bug is gone; it
doesn't re-plan or re-scope.

Spawn `runtime-integrity-reviewer` **in the same message**, always. Bend is
Netty code: a fix that closes one defect can open another (a `ByteBuf` no
longer released on the new early-return, an exception now swallowed in
`exceptionCaught`, a write whose `ChannelFuture` failure nobody hears of,
blocking work moved onto the event loop). `precedent-reviewer` stays off: a
bug fix has no plan prose to drift from.

The review-branch skill dir is
`"$(git rev-parse --show-toplevel)/.claude/skills/review-branch"`. If it is
missing, skip the reviewer and say so: the verifier's gate doesn't depend on
it.

```
Agent({
  subagent_type: "bug-fix-verifier",
  prompt: `Verify the bug fix on this branch.

Bug directory: docs/issues/<branch-name>/

Read BUG.md, re-run the repro against HEAD, confirm the bug no longer
reproduces, and glance at touched files for obvious regressions. Write
your verdict into the BUG.md Verification section and report back with
a score (1-10). Threshold: 7.`
})

Agent({
  subagent_type: "runtime-integrity-reviewer",
  prompt: `Full-branch review, runtime-integrity lane, on a bug-fix branch.

Skill dir: <review-branch skill dir>
Plan directory: docs/issues/<branch-name>/
Diff base: origin/development

FIRST: read <skill dir>/references/finder-base.md, then your three corpus sections under
<skill dir>/references/corpus/. Return ## Findings and ## Checked only, in the finder shape.
No score, no severity, no waiver applied. Do NOT re-verify the reproduction: bug-fix-verifier
owns that.`
})
```

Bend's hazard list and accepted-failure classes live in `finder-base.md`, so
the spawn prompt doesn't carry them.

When the reviewer returns, append its finder report **below** the verifier's
block under `## Verification`, as a `### Runtime integrity` subsection
carrying its `## Findings` and `## Checked` verbatim. Below, not above: `/pr`
reads the **first** `**Verdict:**` line under `## Verification` as the
verifier's verdict, and the reviewer's block must never be the first thing
there. Do not judge, route, or write READY / NEEDS WORK. The parent session
sees the findings and decides what to do; they never block the verdict
below.

- **Score ≥ 7 AND Verdict reads `PASS`**: proceed to Phase 9. Update Status
  to `Verified`. Runtime-integrity findings do not block this.
- **Score < 7, OR Verdict reads `PARTIAL`/`FAIL`, OR "Required fixes before
  PR" is non-empty**: fix what the verifier found, re-run your repro,
  re-spawn. Don't hand off to `/pr` until the verdict reads a clean `PASS`
  with no outstanding required fixes. The score and the verdict are
  independently-written fields; a high score doesn't override a verdict or a
  required-fix list that says otherwise. Shipping a "fix" that still
  reproduces is worse than shipping no fix: it erodes trust in the issue
  tracker.

If the verifier can't run the repro for environmental reasons (Gradle can't
resolve, JDK 25 toolchain missing), surface it to the user. Don't lower the
threshold.

## Phase 9: Commit and hand off to /pr

Commit with `/commit` (`fix(<scope>): <summary> (BD-NNNN)`), then run `/pr`.
`/pr` pushes, posts the standard feedback comment on the linked Kendo issue,
and embeds `bug-fix-verifier`'s verdict from BUG.md's `## Verification`
section as the gate. Bug fixes are gated by the verifier, not by the pre-PR
finders, so `/pr` will not ask for a `/review-branch` handoff on a bug
branch.

PR title names the defect (`fix(protocol): reject zero-length frame encoded
as 0x80 0x00`) rather than the issue key alone. PR body cites the issue key
plus a one-paragraph summary lifted from BUG.md's Fix section so the change
reads without opening the file.

## Phase 10: Post-mortem prompt (optional)

Once `/pr` is open and the verifier has signed off, you have more information
than when Phase 3 started. Spend 30 seconds asking: **what would have
prevented this bug?**

If the answer is structural (no good test seam, flagged in BUG.md Notes; the
built-in and endpoint send paths tangled again; a class of bug that keeps
recurring at the same boundary, like a length prefix sizing an allocation
before a readable check), surface it via `AskUserQuestion`:

> Would a structural fix help here? Reason: `<one specific line, e.g. "no
> seam to drive the handler without a real socket" or "second length-prefix
> pre-allocation bug in the packet readers">`.

If "yes," file it as a Kendo issue with the specific pointer, or hand it to
`/plan-feature`. A concrete starting point beats "improve something."

Also ask whether the bug's lesson is a lasting rule. If it is, record the
reasoning in the branch's `DECISIONS.md` (or update the ADR it changes, under
`docs/decisions/`) and add a one-line rule to the matching `CLAUDE.md`, root or
module. That is where Bend records why things are built the way they are.

**Skip this phase entirely for path 3b mechanical fixes** (typos,
off-by-ones, missing null checks). Asking after every trivial fix turns the
prompt into noise the developer learns to ignore.
