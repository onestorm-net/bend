---
name: kendo
description: |
  Kendo issue tracking for Bend through the local `kendo` MCP server (tenant onestorm, project
  Bend, project_id 1). Use when creating, updating, moving, or searching BD issues, writing
  issue descriptions, managing epics (Goal, Done when wishes and their ticks, What done looks
  like) and sprints, commenting on issues or epics, setting labels, linking branches to issues,
  logging time, or recording token usage. Always the `mcp__kendo__*` tools, never the claude.ai
  `Kendo-*` connectors.
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
| `kendo://projects/1/epics` | List epics: `get-epics-tool`'s list form plus `issues_count`, without issue rows |
| `kendo://projects/1/github-repos` | List linked GitHub repositories |
| `kendo://issues/{id}` | Get a single issue with details; takes the database id or the key (`kendo://issues/BD-0012`) |

## MCP Tools

### Issues

| Tool | Purpose |
|------|---------|
| `mcp__kendo__create-issue-tool` | Create new issue (`label_ids`, `blocked_by_ids`, `blocks_ids`: at most 500 each) |
| `mcp__kendo__bulk-create-issues-tool` | Create 1-25 issues in one call, all in the backlog and one lane. Rows take `title`, `description`, `priority`, `type` only; one malformed row rejects the whole call. Returns `keys` and per-row `failed` |
| `mcp__kendo__update-issue-tool` | Update issue fields (including lane changes via `lane_id`). Blocking lists replace the stored ones: omit to keep, `[]` to clear |
| `mcp__kendo__delete-issue-tool` | Delete issue (destructive) |
| `mcp__kendo__link-branch-tool` | Link a git branch to an issue |
| `mcp__kendo__unlink-branch-tool` | Remove a git branch link from an issue |
| `mcp__kendo__prepare-project-context-tool` | Read-only bundle: `project + lanes + sprints (Planned/Active) + active_sprint shortcut + labels + members + current_user` in one call. The gather step for any project-scoped flow |
| `mcp__kendo__prepare-issue-context-tool` | Read-only bundle: `issue + epic` (full issue payload with comments, branches, attachments; the epic with its `goal`, description, derived status, `checks` and `wishes`), by `issue_key` or `issue_id` |
| `mcp__kendo__search-issues-tool` | Search, filter or count issues; see [Search Issues](#search-issues) |
| `mcp__kendo__get-my-issues-tool` | Your assigned issues, done lane excluded (filter by `project_id`, `priority`, `type`, `query`). Capped at 500: check `truncated` |
| `mcp__kendo__kendo-today-tool` | Your Today snapshot in one read: active sprints, open issues per project, today's logged minutes. Prefer it over `get-my-issues` for "what's on my plate" |
| `mcp__kendo__start-work-on-issue-tool` | Idempotent one-call write: assigns user, moves to lane, optionally updates sprint, and links a git branch. The repo resolves because Bend has exactly one (`onestorm-net/bend`) |

### Labels

| Tool | Purpose |
|------|---------|
| `mcp__kendo__get-labels-tool` | The project's labels (`id`, `name`, `color` 0-9, `order`). `prepare-project-context-tool` carries them too |
| `mcp__kendo__create-label-tool` | Create a label (`name` unique in the project, `color` 0-9) |
| `mcp__kendo__update-label-tool` | Rename or recolor a label |
| `mcp__kendo__delete-label-tool` | Delete a label; issues keep everything but the label (destructive) |
| `mcp__kendo__sync-issue-labels-tool` | Set an issue's labels: `label_ids` replaces the set exactly, `[]` clears it |

### Comments

Each comment tool names its subject with exactly one of `issue_id`, `issue_key` or `epic_id`;
none, or more than one, is refused.

| Tool | Purpose |
|------|---------|
| `mcp__kendo__add-comment-tool` | Comment on an issue or an epic (max 10,000 chars). `parent_id` replies to a top-level comment on the same subject; a reply to a reply is refused |
| `mcp__kendo__update-comment-tool` | Edit a comment. Find its ID in `kendo://issues/{id}`, or in `get-epics-tool` with `epic_id` |
| `mcp__kendo__delete-comment-tool` | Delete a comment and its attachments (destructive) |

### Epics

| Tool | Purpose |
|------|---------|
| `mcp__kendo__get-epics-tool` | Get epics with their issues and progress (filter by derived `status`: 0=Open, 1=Closed). With `epic_id`, also the description, `checks`, `wishes` and comments; see [Epics in Detail](#epics-in-detail) |
| `mcp__kendo__create-epic-tool` | Create an epic: title, color, `goal`, `assignee_id`, dates, `issue_ids`, `checks`, `wishes` |
| `mcp__kendo__update-epic-tool` | Update an epic's fields, issues, `checks` or `wishes`. Status is derived, not set |
| `mcp__kendo__delete-epic-tool` | Delete an epic and its comments (destructive). Issues are kept. |
| `mcp__kendo__tick-epic-wish-tool` | Tick one Done when wish (`epic_id`, `wish_id`); records who and when |
| `mcp__kendo__untick-epic-wish-tool` | Untick one Done when wish; clears who and when |

An issue joins an epic via `epic_id` on create/update. Don't create an epic with nothing to
attach to it (ADR 0023 records why there is no `Protocol` epic).

### Sprints

| Tool | Purpose |
|------|---------|
| `mcp__kendo__create-sprint-tool` | Create a new sprint (title, start/end dates). Created as Planned. |
| `mcp__kendo__update-sprint-tool` | Update sprint title, dates, or status (partial updates supported) |
| `mcp__kendo__complete-sprint-tool` | Complete the active sprint, move incomplete issues to target sprint |
| `mcp__kendo__delete-sprint-tool` | Delete a Planned sprint; its issues leave the sprint (destructive) |
| `mcp__kendo__assign-issues-to-sprint-tool` | Bulk-assign up to 100 issues to a sprint (or `null` to clear) |
| `mcp__kendo__get-sprints-tool` | List sprints (all statuses, or filter by `status` 0/1/2). Use for Completed sprints, which `prepare-project-context` excludes |

### Time and Token Usage

| Tool | Purpose |
|------|---------|
| `mcp__kendo__get-time-entries-tool` | Query time entries between `date_from` and `date_to` (filter by project, user, issue; group by issue/day, or `none` for raw entries, at most 100). An entry counts on the day its work started |
| `mcp__kendo__create-time-entry-tool` | Log time on an issue: `minutes_spent` (at least 1), optional `started_at` and `note` |
| `mcp__kendo__update-time-entry-tool` | Correct your own entry: minutes, note, start, or move it to another issue in the project |
| `mcp__kendo__delete-time-entry-tool` | Delete your own entry (destructive) |
| `mcp__kendo__record-token-usage-tool` | Record one session's token usage for one model on an issue (`model`, `tokens_in`, `tokens_out`, optional cache counts). Pass `external_event_id` (`<session id>:<model>`) so a resend replaces instead of adding. Send measured counts, never estimates |

### Reports and Feedback

| Tool | Purpose |
|------|---------|
| `mcp__kendo__get-error-groups-tool` | Recurring exceptions captured from the apps, newest first. Bend ships no app reporting to Kendo yet, so expect nothing |
| `mcp__kendo__send-feedback-tool` | Send feedback about Kendo itself to the Kendo team (`title`, `description`). Not for Bend's own issues |

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
issue_key: "BD-0012"          # or issue_id: <issue-id>
branch_name: "BD-0012-endpoint-registry"
```

**Guard:** when the branch name already carries the issue key (every `BD-NNNN-<slug>` branch
does), Kendo auto-links it, and `start-work-on-issue-tool` links it too. Reserve
`link-branch` for a branch whose name does not contain the key.

### Epics in Detail

An epic has a title, a color (`0`-`9`), a **Goal** (`goal`, at most 500 characters, what the epic
delivers), a description, `start` and `end` (sent together), an assignee, `issue_ids`, and two
ordered lists of at most 100 items of at most 500 characters:

- **Done when** (`wishes`): what the epic must deliver. Each is ticked by hand with
  `tick-epic-wish-tool`, and can link issues of the same epic (`issue_ids`).
- **What done looks like** (`checks`): plain-text lines to verify the work against, in the same
  `Proof: …` shape as an issue's section (see
  [issue-templates.md](references/issue-templates.md#what-done-looks-like)). Never ticked.

**Status is derived** (0=Open / 1=Closed); no tool sets it. An epic with wishes is Closed once
every wish is ticked, even with issues still open. Without wishes, it is Closed when it has
issues and every one is in Done.

**Read one epic for the full picture.** The list form (`get-epics-tool` without `epic_id`) sends
the goal, status and counts, but no description, lists or comments. Pass `epic_id` for those.

**A list is saved whole.** `create-epic-tool` and `update-epic-tool` take `checks` and `wishes`
as the complete list, in order:

- An item sent with its `id` keeps its row, and a wish keeps its tick.
- An item without an `id` is created. A stored item left out is deleted, a wish with its tick.
- Omitting the key, or sending `null`, keeps the list; `[]` clears it. `issue_ids` works the same.
- Every issue a wish links must be in the epic once the call's own `issue_ids` apply. One wrong
  link rejects the whole call and nothing is stored.

So before sending a list on `update-epic-tool`, read the epic with `epic_id` and send every kept
item with its `id`. To change only the goal, dates, assignee or issues, leave both lists out.

**Ticking** takes `epic_id` and a `wish_id` from the `epic_id` read. Both tick and untick are
idempotent and answer the epic's new status, so ticking the last open wish answers Closed.

### Log Time

```
issue_key: "BD-0012"          # or issue_id: <issue-id>
minutes_spent: 60             # at least 1
started_at: "2026-10-20T09:00:00+02:00"   # optional; without an offset it is read as UTC
note: "What was done"         # optional
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

Use `ReadMcpResourceTool` with `server: "kendo"` and `uri: "kendo://issues/{id}"`; the URI takes
the key too (`kendo://issues/BD-0012`). Prefer `prepare-issue-context-tool` when you also want
the epic.

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
