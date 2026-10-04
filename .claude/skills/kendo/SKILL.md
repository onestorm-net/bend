---
name: kendo
description: |
  Kendo issue tracking for Bend through the local `kendo` MCP server (tenant onestorm, project
  Bend, project_id 1). Use when creating, updating, moving, or searching BD issues, writing
  issue descriptions, managing epics and sprints, linking branches to issues, or logging time.
  Always the `mcp__kendo__*` tools, never the claude.ai `Kendo-*` connectors.
---

# Kendo

Manage Bend's issues on the Kendo tenant `onestorm` through the **local** MCP server named
`kendo`. Its tools are `mcp__kendo__*`. The claude.ai connectors named `Kendo-script`,
`Kendo-emmie` and similar are other tenants: never use them for Bend. There is no Kendo CLI;
everything goes through MCP.

## Setup

The server is already configured as `kendo`. See [references/setup.md](references/setup.md) only
if the tools are missing or auth has expired.

## Bend project facts

| Thing | Value |
|---|---|
| Project | Bend, `project_id: 1`, linked GitHub repo `onestorm-net/bend` |
| Issue keys | `BD-NNNN`, always 4 digits (`BD-0012`, never `BD-12`) |
| Lanes | To Do = `1`, In Progress = `2`, In Review = `3`, Done = `4` |
| Users | sole user, id `1` |
| Epics | `client-api` = `2`, `client-netty` = `3`, `server-netty` = `4`, `Release hygiene` = `5`, `Claude setup` = `6` |
| Language | issues and comments are written in English |

Lanes and epic ids are stable, but if a call rejects one, re-read them via
`mcp__kendo__prepare-project-context-tool` / `mcp__kendo__get-epics-tool` rather than guessing.

## MCP Resources (Read-Only)

| URI | Description |
|-----|-------------|
| `kendo://projects/1/issues` | List issues |
| `kendo://projects/1/lanes` | List board lanes |
| `kendo://projects/1/sprints` | List sprints (status: 0=Planned, 1=Active, 2=Completed) |
| `kendo://projects/1/epics` | List epics |
| `kendo://projects/1/github-repos` | List linked GitHub repositories |
| `kendo://issues/{id}` | Get a single issue with details (database id, not key) |

## MCP Tools

### Issues

| Tool | Purpose |
|------|---------|
| `mcp__kendo__create-issue-tool` | Create new issue |
| `mcp__kendo__update-issue-tool` | Update issue fields (including lane changes via `lane_id`) |
| `mcp__kendo__add-comment-tool` | Add comment to issue (max 2000 chars) |
| `mcp__kendo__delete-issue-tool` | Delete issue (destructive) |
| `mcp__kendo__link-branch-tool` | Link a git branch to an issue |
| `mcp__kendo__unlink-branch-tool` | Remove a git branch link from an issue |
| `mcp__kendo__prepare-project-context-tool` | Read-only bundle: `project + lanes + sprints (Planned/Active) + active_sprint shortcut + labels + members + current_user` in one call. The gather step for any project-scoped flow |
| `mcp__kendo__prepare-issue-context-tool` | Read-only bundle: `issue + epic` (full issue payload with comments, branches, attachments), by `issue_key` or `issue_id` |
| `mcp__kendo__search-issues-tool` | Search, filter or count issues; see [Search Issues](#search-issues) |
| `mcp__kendo__get-my-issues-tool` | Your assigned issues, done lane excluded (filter by `project_id`, `priority`, `type`, `query`) |
| `mcp__kendo__start-work-on-issue-tool` | Idempotent one-call write: assigns user, moves to lane, optionally updates sprint, and links a git branch. Repo is auto-resolved from the project's linked repo |

### Epics

| Tool | Purpose |
|------|---------|
| `mcp__kendo__get-epics-tool` | Get epics with full issue details and progress (filter by status) |
| `mcp__kendo__create-epic-tool` | Create a new epic (with color, dates, linked issues) |
| `mcp__kendo__update-epic-tool` | Update epic details, status, dates, or issue assignments |
| `mcp__kendo__delete-epic-tool` | Delete an epic (destructive). Issues are kept. |

An issue joins an epic via `epic_id` on create/update. Don't create an epic with nothing to
attach to it (CLAUDE.md records why there is no `Protocol` epic).

### Sprints

| Tool | Purpose |
|------|---------|
| `mcp__kendo__create-sprint-tool` | Create a new sprint (title, start/end dates). Created as Planned. |
| `mcp__kendo__update-sprint-tool` | Update sprint title, dates, or status (partial updates supported) |
| `mcp__kendo__complete-sprint-tool` | Complete the active sprint, move incomplete issues to target sprint |
| `mcp__kendo__assign-issues-to-sprint-tool` | Bulk-assign up to 100 issues to a sprint (or `null` to clear) |
| `mcp__kendo__get-sprints-tool` | List sprints (all statuses, or filter by `status` 0/1/2). Use for Completed sprints, which `prepare-project-context` excludes |

### Time Logging

| Tool | Purpose |
|------|---------|
| `mcp__kendo__get-time-entries-tool` | Query time entries (filter by issue, sprint; group by issue/day) |
| `mcp__kendo__create-time-entry-tool` | Log time spent on an issue (1-479 minutes) |

### Attachments

| Tool | Purpose |
|------|---------|
| `mcp__kendo__fetch-attachment-tool` | Fetch attachment bytes by ID: image block for `image/*`, text block for `text/*` + `application/json`. Capped at 5 MB images / 500 KB text; binary types return an error with a `download_url` hint. |

## Common Operations

### Create Issue

Write the description with [references/issue-templates.md](references/issue-templates.md).

```
project_id: 1
title: "Clear descriptive title"
description: "Markdown content"
lane_id: 1 (To Do; optional, defaults to first lane)
priority: 0-4 (0=Highest, 2=Medium default, 4=Lowest)
type: 0-2 (0=Feature, 1=Bug, 2=Task)
epic_id: 2-5 (optional, see Bend project facts)
assignee_id: 1 (optional)
sprint_id: <sprint_id> (optional)
```

**Important:** The response includes both `id` (database id, e.g. `12`) and `key` (display
reference, e.g. `BD-0012`). These are different values: use `key` for branch names, commit
messages and user-facing references, and `id` for API calls to other tools.

### Move Issue (via update-issue)

```
issue_id: <issue-id>
lane_id: 2   # In Progress; 3 = In Review, 4 = Done
```

### Start Work (assign + lane + sprint + branch link)

```
issue_key:    "BD-0012"
branch_name:  "BD-0012-endpoint-registry"
assignee_id:  1
lane_id:      2
sprint_id:    <active sprint id>   # omit when there is none; null clears the sprint
```

`/newbranch` and `/prepare-issue` wrap this.

### Link Branch

```
issue_id: <issue-id>
branch_name: "BD-0012-endpoint-registry"
```

**Guard:** when the branch name already carries the issue key (every `BD-NNNN-<slug>` branch
does), Kendo auto-links it, and `start-work-on-issue-tool` links it too. Reserve
`link-branch` for a branch whose name does not contain the key.

### Log Time

```
issue_id: <issue-id>
time_spent: 60  # minutes (1-479)
```

### Parallel-gather pattern

When a flow needs **both** project meta and issue payload (e.g. "pick up BD-0012"), fire both
gather tools in the same tool-call block:

```
parallel:
  mcp__kendo__prepare-project-context-tool(project_id: 1)
  mcp__kendo__prepare-issue-context-tool(issue_key: "BD-0012")
```

Use only the issue tool when the project facts above are all you need.

### Get Issue by ID

Use `ReadMcpResourceTool` with `server: "kendo"` and `uri: "kendo://issues/{id}"` when you have
the database id. Prefer `prepare-issue-context-tool` when you have the key.

### Search Issues

Use `mcp__kendo__search-issues-tool` with `project_id: 1` to find, filter or count issues.
`query` searches title, description and issue key; it does not match numeric database ids.

- **Lists match any value.** `lane_ids`, `assignee_ids`, `sprint_ids`, `epic_ids`, `label_ids`,
  `priorities`, `types`. The scalar forms (`lane_id`, `priority`, …) still work and merge into the list.
- **`exclude_*` leaves issues out** (`exclude_lane_ids`, `exclude_types`, …). An exclusion wins
  over an inclusion of the same value.
- **`0` means none** in assignee, sprint and epic lists: unassigned, backlog, no epic. An
  exclusion keeps issues without a value unless `0` is in it, so `exclude_epic_ids: [0]` returns
  only issues with an epic.
- **`group_by` counts instead of listing** (`lane`, `assignee`, `sprint`, `epic`, `priority`,
  `type`, `label`). It returns `total` plus `groups` of `{key, label, count}`; the "none" group
  has key `0`.
- **`include_description: false`** drops each description. Use it for any list you only scan.
- **`limit`** is 1-100, default 25. Check `truncated` before treating a list as complete.

Each row names its lane, assignee, sprint and epic next to their ids. Answer the user from the
names and feed the ids to the next call.

```
# Open issues per epic, excluding Done
search-issues-tool  project_id: 1, group_by: "epic", exclude_lane_ids: [4]

# Everything left in the client-netty epic, light rows
search-issues-tool  project_id: 1, epic_ids: [3], exclude_lane_ids: [4], include_description: false
```

## References

| File | Content |
|------|---------|
| [setup.md](references/setup.md) | What to check if the `kendo` MCP server is missing or unauthenticated |
| [issue-templates.md](references/issue-templates.md) | Issue templates (feature, bug, task) with Bend examples. Single source of truth, used by `/newbranch`, `/plan-feature`, `/task-writer` and any skill that creates issues |
