---
name: prepare-issue
description: >
  Prepare a Kendo issue for development: assign it, create (or reuse) its feature branch, link
  the branch, add it to the active sprint and move it to In Progress.
  Usage: /prepare-issue <issue_key_or_id_or_url>. Use whenever the user wants to start working
  on an issue, prepare an issue, pick up an issue, or says "prepare issue", "start issue",
  "work on issue". Also trigger when combining issue assignment with branch creation.
argument-hint: "<issue-key-or-id-or-url>"
arguments: [issue]
allowed-tools: Bash(git *) Bash(gh *) Read Grep Glob
---

Task preparation assistant. Argument: `$issue` (required).

Bend facts this skill relies on: Kendo project `project_id: 1` on the local `kendo` MCP server
(`mcp__kendo__*` tools only; there is no Kendo CLI); lane In Progress = `2`; sole user id `1`;
issue keys `BD-NNNN` (4-digit); branches `BD-NNNN-<kebab-slug>` from `origin/development`.

- `$issue` accepts three forms:
  - **Issue key** (e.g. `BD-0012`): used verbatim.
  - **Numeric ID** (e.g. `12`): used verbatim against `issue_id` in Step 1.
  - **Kendo URL** (e.g. `https://onestorm.kendo.dev/projects/1/issues/BD-0012`): extract the
    segment after `/issues/` and use that as the key. Strip any trailing `?…` query string or
    `#…` fragment.

**Normalise `$issue` before Step 1.** If `$issue` starts with `http://` or `https://`, set the
working issue value to the segment after `/issues/` (stripped of any `?` query or `#` fragment);
otherwise use `$issue` verbatim. All later steps reference the normalised value.

## Pre-resolved context

- **Current branch:** !`git branch --show-current`

Available immediately; do not re-run this command during the steps below.

**Error handling:** If any step fails, report the error clearly and stop; do not continue to the
next step. Then ask the user whether they want to reverse the steps that already completed.

If the user wants to reverse, undo completed steps in reverse order:

| Completed step | Reversal |
|----------------|----------|
| Started work on issue (assignee + lane + sprint + branch link) | `mcp__kendo__update-issue-tool` to restore the original `assignee_id` / `lane_id` / `sprint_id`; `mcp__kendo__unlink-branch-tool` to detach the branch |
| Created branch | `git checkout {previous_branch}` then `git branch -D {branch_name}` (it was never pushed) |

## Step 1: Bundle the issue and project context (parallel)

```
parallel:
  mcp__kendo__prepare-project-context-tool
    project_id: 1
  mcp__kendo__prepare-issue-context-tool
    issue_key: "BD-0012"     # or use issue_id when the argument is purely numeric
```

If the issue does not exist or returns an error, inform the user and stop. Otherwise capture:

From `prepare-issue-context`:
- `issue.id`, `issue.key`, `issue.title`, to confirm with the user and to drive branch naming
- `issue.type`: `0` = Feature, `1` = Bug, `2` = Task. Used by Step 5's bug-workflow hint.
- current `assignee_id` / `lane_id` / `sprint_id`, needed only for the reversal table above

From `prepare-project-context`:
- `active_sprint.id`, for Step 3A; `null` means skip sprint (omit it, don't pass `null`)

Confirm with the user: "Prepare **{key}**: *{title}*?"

## Step 2: Check for an existing branch

Before creating a new branch, check whether one for this issue already exists. Branches may have
been created by hand, so search broadly:

```bash
git branch --list -i "*BD-{issue_number}*" "*BD-0{issue_number}*" "*BD-00{issue_number}*" "*BD-000{issue_number}*"
```

Where `{issue_number}` is the numeric part of the key without leading zeros (e.g. `12` from
`BD-0012`). This catches `BD-12-...`, `BD-0012-...` and similar.

Also check whether the **current branch** might be related to the issue. Three outcomes:

### A. Clear match: a branch contains the issue number

Skip `/newbranch`. Check out the existing branch (if not already on it), then go to Step 3A.

### B. Possible match: the current branch has no issue number, but its name overlaps with the issue title

Use `AskUserQuestion`:

> "The current branch `{branch_name}` looks like it might be related to **{issue_key}**:
> *{issue_title}*. Is this the branch for this issue, or should I create a new one?"

Options: **Use this branch** / **Create new branch**

"Use this branch" → treat as case A. Otherwise → Step 3B.

### C. No match

Go to Step 3B.

## Step 3: Start work

### 3A. Existing branch

One idempotent call aligns the issue state with the branch:

```
mcp__kendo__start-work-on-issue-tool
  issue_key:    "BD-0012"
  branch_name:  "{existing branch name}"
  assignee_id:  1
  lane_id:      2
  sprint_id:    <active_sprint.id from Step 1>   # omit when active_sprint is null
```

The tool no-ops per field (already assigned, already on the lane, branch already linked all
short-circuit), so it's safe to run even when most of the state is already correct.

### 3B. New branch via /newbranch

Invoke `/newbranch` with the issue key (e.g. `BD-0012`) as argument. It handles:
- Fetching `origin/development`
- Creating `BD-NNNN-<slug>` from `origin/development` with `--no-track` (no push)
- Calling `start-work-on-issue-tool` to assign, move to In Progress, add to the active sprint,
  and link the branch

After `/newbranch` completes, note the **branch name** it created.

## Step 4: Planning folder hint

The branch name is also the planning folder: `docs/issues/<branch-name>/` (see
[`.claude/references/plan-directory.md`](../../references/plan-directory.md)). Don't create it
here; `/plan-feature` or `/fix-bug` does.

## Step 5: Confirm

Print a summary:

```
Issue #{issue_id} ({key}) prepared!
  [x] Assigned to user 1
  [x] Branch: {branch_name} (from origin/development, not pushed)
  [x] Branch linked to issue
  [x] Added to sprint {sprint}          ← only if there is an active sprint
  [x] Moved to In Progress
```

Then suggest the next step by issue type (this skill doesn't run it):

```
  → Bug report detected. Next: /fix-bug {key}         ← type 1
  → Next: /plan-feature {key}                         ← type 0 or 2
```
