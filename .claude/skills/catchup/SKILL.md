---
name: catchup
description: |
  Load the full context of the current branch and check alignment with development: git diff summary,
  commit history, plan/bug/task files in docs/issues/<branch>/, decisions, and the Kendo issue
  details. Reports how far behind development the branch is and offers to merge on confirmation. Resolves merge conflicts intelligently (auto-resolves obvious ones, asks on
  ambiguous ones). Use this skill whenever the user wants to understand what the current branch
  is about, needs context after a /clear, starts a new session, says "what am I working on",
  "load context", "catch me up", "branch context", "where was I", "summarize this branch",
  "catchup", "sync my branch", "align with development", "update branch", or any variant of wanting to
  understand or sync the current state of work. Also use when the user just passes a branch name
  or issue key and expects you to load its context. Trigger on session start when the branch is
  not `development` or `main` and the user's first message implies they want to resume work.
---

# Branch Catchup

Load everything about the current branch, check alignment with `development`, and produce a concise
working summary so you (and the user) can hit the ground running.

The base is always `development`. The tracker is Kendo via the local `kendo` MCP server
(`mcp__kendo__*` tools only), and planning artifacts live in `docs/issues/<branch-name>/`.

## Step 1: Identify the branch, base, and issue key

1. Run `git branch --show-current` to get the branch name
2. If on `development` or `main`, skip alignment reporting and just output context
3. Extract the issue key from the branch name (`BD-\d{4}`, e.g. `BD-0007` from
   `BD-0007-claude-skills`); Step 2's Kendo lookup uses it.
4. Resolve the branch's directory with
   [`.claude/references/plan-directory.md`](../../references/plan-directory.md) (shared with
   `next`, `task-writer`, `commit`, `review-branch` and `pr`). It tries
   `docs/issues/<branch-name>/` first, then a single folder starting with the issue key. Use its
   `catchup` fallback instead of asking: report "no plan directory"; several key-prefix matches →
   name them all in the summary rather than reading one. Whether the directory holds `PLAN.md` or
   `BUG.md` tells you if this is a plan or a bug branch.

## Step 2: Gather context (run all in parallel)

Collect these data sources simultaneously:

| Source | Command / path | What to capture |
|--------|---------------|-----------------|
| **Commits** | `git log origin/development..HEAD --oneline` | All commits on this branch |
| **Diff stat** | `git diff origin/development...HEAD --stat` | Files changed + insertions/deletions |
| **TASKS.md** | `<dir>/TASKS.md` | Progress: count completed vs total tasks |
| **PLAN.md** | `<dir>/PLAN.md` | High-level plan summary (first ~50 lines) |
| **BUG.md** | `<dir>/BUG.md` | Bug branches: problem, cause, and the first `**Verdict:**` under `## Verification` if present |
| **DECISIONS.md** | `<dir>/DECISIONS.md` | Key design decisions and their status |
| **Kendo issue** | `mcp__kendo__prepare-issue-context-tool` with `issue_key: "BD-NNNN"` | Issue title, description, lane, assignee (only if the branch has an issue key) |
| **Divergence** | `git fetch origin development && git rev-list --left-right --count origin/development...HEAD` | Commits behind/ahead of `development` |

If any source doesn't exist (no directory, no DECISIONS.md, no issue key), skip it silently;
don't error.

## Step 3: Output a working summary

Present the summary in this format. Keep it concise, not a wall of text:

```
## Branch: <branch-name>
**Issue:** <BD-NNNN>: <issue title>               ← only if the Kendo lookup produced data
**Lane:** <lane name from Kendo>                    ← only if the Kendo lookup produced data

### Alignment with development
<"Up to date" OR "X commits behind, say 'sync' to merge">

### What this branch does
<2-3 sentence summary derived from PLAN.md or BUG.md and the commits>

### Progress
<X/Y tasks complete>
- [x] Completed task summaries (one line each)
- [ ] **Next up:** <first incomplete task with brief description>
- [ ] Remaining tasks (one line each)

### Key decisions                                  ← only if DECISIONS.md exists
<Only list decisions that matter for continuing work; skip obvious or already-resolved ones>

### Recent commits (last 5)
<short log>

### Files changed
<diff stat summary, e.g. "12 files changed across protocol/ and client-api/">
```

**Important:**
- The summary should be scannable in under 30 seconds
- Don't reproduce full plan text; summarize it
- For tasks, show the status and what's next, not the full task details
- For decisions, only surface ones that affect how to continue (skip resolved/obvious ones)
- If all tasks are complete, say so and suggest next steps (`/review-branch`, `/pr`)
- Always report the alignment status: the user should know whether they're on stale code

## Step 4: Align with development (only when the user confirms)

Do NOT merge automatically. After showing the summary, if the branch is behind `development`, tell
the user how many commits behind they are and wait for confirmation (e.g., "sync", "merge it",
"yes", "go ahead").

When the user confirms alignment:

### 4a. Handle uncommitted changes

Run `git status --porcelain`. If there are uncommitted changes:
```bash
git stash push -m "catchup-auto-stash"
```
Remember to pop the stash after the merge completes.

### 4b. Merge development

```bash
git merge origin/development --no-edit
```

Using merge (not rebase) because it's non-destructive and safe for branches already pushed to
origin. No force-push needed, history is preserved.

### 4c. If the merge succeeds cleanly

Report how many commits were pulled in and move on.

### 4d. If there are merge conflicts, resolve them

For each conflicting file:

1. **Read the conflict markers**: understand what both sides changed and why
2. **Check the git log for both sides**: `git log --oneline origin/development -- <file>` and
   `git log --oneline HEAD -- <file>` to understand the intent behind each change
3. **Read the surrounding code**: understand the broader context of the conflicting area
4. **Resolve based on clarity:**
   - **Clear-cut conflicts** (one side didn't touch the area, or changes are in different logical
     sections) → resolve automatically:
     - `development` changed something the feature branch didn't touch nearby → take `development`'s version
     - Feature branch changed something `development` didn't touch nearby → keep the feature branch version
     - `gradle/libs.versions.toml` version bumps on both sides → take the higher version unless
       the branch pinned one on purpose (check its commit message), then rerun `./gradlew build`
     - `CLAUDE.md` / `protocol/protocol-v1.md` → never auto-resolve if both sides edited the same
       section; these are design records, ask
   - **Ambiguous conflicts** (both sides changed the same logic, or changes seem contradictory) →
     describe the conflict to the user and ask for guidance before resolving. Show what each side
     intended and propose a resolution, but wait for confirmation.
5. **Stage the resolved file**: `git add <file>`

After all conflicts are resolved:
```bash
git commit --no-edit
```

If any resolved file is Java source or Gradle config, run `./gradlew build` before reporting: two
sides that were each green can still merge into a broken build. Do not push; `/pr` pushes.

### 4e. Restore stashed changes

If changes were stashed in step 4a:
```bash
git stash pop
```
If the stash pop itself conflicts, resolve those using the same strategy.

### 4f. Report the result

```
### Alignment complete
Merged X commits from development (clean)
```
Or if conflicts were resolved:
```
### Alignment complete
Merged X commits from development, resolved conflicts in: VarInt28FrameDecoder.java, CLAUDE.md
```
