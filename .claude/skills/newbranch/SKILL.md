---
name: newbranch
description: >
  Create a new branch from origin/main for a Kendo issue: resolve or create the BD issue first,
  then create the branch locally and prepare the issue for work (assign, add to the active
  sprint, move to In Progress, link the branch). Never pushes. Use whenever the user wants to
  start a new feature, create a branch, begin work on an issue, or says "new branch".
---

# New Branch

Create a new git branch from `origin/main` and prepare the linked Kendo issue for work (assign to
the developer, add to the active sprint, move to In Progress, link the branch).

Bend facts this skill relies on: integration branch `main`; Kendo project `project_id: 1` via the
local `kendo` MCP server (`mcp__kendo__*` tools only); lane In Progress = `2`; sole user id `1`;
issue keys `BD-NNNN` (4-digit).

## Step 1: Fetch latest

```bash
git fetch origin main
```

## Step 2: Find or create the issue

Ask the user which issue to branch from. Options:

- **User provides an issue number or key** (`BD-0012`, `12`): resolve it with
  `mcp__kendo__prepare-issue-context-tool` and read its `key` field.
- **User provides a search query**: `mcp__kendo__search-issues-tool` with `project_id: 1`.
- **User wants a new issue**: create one with `mcp__kendo__create-issue-tool`, `project_id: 1`,
  following [`../kendo/references/issue-templates.md`](../kendo/references/issue-templates.md)
  (feature, bug and task shapes for this project). Don't improvise a structure: future searches
  of the backlog rely on it being consistent.

Fire the gather tools in one parallel block:

```
parallel:
  mcp__kendo__prepare-project-context-tool
    project_id: 1
  mcp__kendo__prepare-issue-context-tool
    issue_key: "<issue-key>"
```

From `prepare-issue-context`: `issue.key` / `issue.title`, the branch slug source.

From `prepare-project-context`: `active_sprint`, its `id` for Step 4, or `null` if the project
has no Active sprint (skip sprint then). Lane and assignee are fixed (In Progress = `2`, user
`1`), so there is nothing to pick from `lanes[]` or `members[]`.

## Step 3: Create the branch

Use the issue `key` plus a kebab-case summary of the title (max ~5 words):

```bash
git checkout -b <issue-key>-short-description --no-track origin/main
```

**Why `--no-track`:** Git's default `autosetupmerge=always` makes the new local branch inherit
`origin/main` as its upstream. Without `--no-track`, a later bare `git push` would push straight
to `main` and bypass PR review. `--no-track` breaks that link; `/pr` later sets the branch's own
upstream with `git push -u origin HEAD`.

**Do not push.** Only `/pr` pushes.

Branch naming rules:
- Prefix with the full 4-digit key (`BD-0012`, not `BD-12`): a truncated key can silently break
  Kendo's auto-link.
- Append a kebab-case slug from the issue title: `BD-0012-endpoint-registry`.
- No slashes. After the uppercase key, only lowercase letters, numbers and hyphens.

The branch name is also the planning folder name: `docs/issues/<branch-name>/` (see
[`.claude/references/plan-directory.md`](../../references/plan-directory.md)).

## Step 4: Start work on the issue

One idempotent write that bundles the state changes. The GitHub repo (`onestorm-net/bend`) is
auto-resolved from the project's linked repo; don't pass it.

```
mcp__kendo__start-work-on-issue-tool
  issue_key:    "<issue-key>"
  branch_name:  "<issue-key>-short-description"
  assignee_id:  1
  lane_id:      2
  sprint_id:    <active_sprint.id from Step 2>   # omit when active_sprint is null
```

**Sprint semantics:** omitting `sprint_id` preserves the existing value; passing `null` clears
the sprint; passing an id sets it. So omit when there is no active sprint; don't pass `null`.

**Idempotency:** re-running with the same inputs is a no-op per field, so it's fine to retry.

## Step 5: Confirm

Tell the user in one concise block:
- The branch name (local only, not pushed yet)
- The issue key + title
- The active sprint it was added to (or "no active sprint, skipped")
- That the issue is assigned and now In Progress, with the branch linked
