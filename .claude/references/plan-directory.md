# Plan directory derivation

Canonical algorithm for resolving a branch's work directory, `docs/issues/<branch-name>/`, from
the current git branch. Every skill in this repo that *finds* an existing plan or bug directory
uses it: `catchup` to summarise the branch's artifacts; `next` to locate `TASKS.md`;
`task-writer` to find where `PLAN.md` already lives so it writes `TASKS.md` alongside it (falling
back to creating the directory, see `task-writer`'s own Output section, only when the algorithm
finds nothing); `commit` to keep `PLAN.md` / `DECISIONS.md` in step with the diff; and
`review-branch` and `pr` read-only, to tell a plan branch from a bug branch.

## Root and folder name

There is one root: `docs/issues/`. Plans and bugs both live there. The folder name is the exact
branch name (`BD-NNNN-<kebab-slug>`), which is what `newbranch` creates and what the writing
skills (`plan-feature`, `fix-bug`, `task-writer`) mint, so writers and readers can't disagree.

## Algorithm

1. Run `git branch --show-current` to get the branch name.
2. Look for `docs/issues/<branch-name>/`.
3. If that directory does not exist, extract the leading issue key (`BD-\d{4}`, e.g. `BD-0007`)
   from the branch and search `docs/issues/` for directories whose name starts with it. If
   exactly one match is found, use it. Several matches (one issue split across more than one
   folder) → don't pick one; go to step 4. This step also covers resolving a directory by issue
   key when the branch isn't checked out.
4. If still nothing matches, ask the user where the work lives. Don't guess. (A skill's own
   fallback below can replace this step.)

## Plan or bug

Which pipeline a directory belongs to is decided by the file it holds, never by its location:

| Directory contains | Kind | Companion files |
|---|---|---|
| `PLAN.md` | plan-driven branch | `DECISIONS.md`, `TASKS.md` |
| `BUG.md` | bug branch | `TASKS.md` when the fix was planned out |
| neither | no plan | (treat as no directory found) |

**If both `PLAN.md` and `BUG.md` exist, the plan wins** and the branch is treated as
plan-driven. That case means a branch carries both a plan and a bug investigation; the plan is
the richer artifact.

## Examples

| Branch | `docs/issues/` contains | Resolves to |
|---|---|---|
| `BD-0007-claude-skills` | `BD-0007-claude-skills/` | `docs/issues/BD-0007-claude-skills/` (step 2) |
| `BD-0012-endpoint-registry` | only `BD-0012-endpoint-registry-dispatch/` | `docs/issues/BD-0012-endpoint-registry-dispatch/` (step 3 prefix match) |
| `BD-0015-keepalive` | `BD-0015-keepalive-timeout/` and `BD-0015-keepalive-echo/` | step 4: two prefix matches, neither is picked |
| `main` | (anything) | step 4: no issue key to match |

## Skill-specific fallbacks

- **`catchup`**: replaces step 4. It summarises, it doesn't execute, so it never asks. Reports
  "no plan directory". Several step-3 matches → lists them all in the summary rather than
  reading one.
- **`next`**: asks the user (step 4 as written).
- **`commit`**: no directory found → nothing to keep in step; commit the code only.
- **`review-branch`**: still spawns the finders; none of them requires `PLAN.md`. It reads
  whichever of `PLAN.md` / `BUG.md` it found for context. It reports in chat on every branch
  shape and writes nothing, so a missing directory changes the context the finders get, not the
  deliverable.
- **`pr`**: on a plan-driven branch, asks whether to run `/review-branch` (default no); a
  missing review does not block. On a bug branch it reads `bug-fix-verifier`'s verdict from
  `BUG.md` instead and never asks for `/review-branch`. With no directory it skips the finder
  check.

## Writers

`plan-feature` and `fix-bug` create `docs/issues/<branch-name>/` rather than resolve it, writing
`PLAN.md` or `BUG.md` respectively, and `task-writer` does the same when the algorithm finds
nothing. They don't run this algorithm, but because the folder is named after the branch (as
`newbranch` names it), step 2 hits exactly on every later lookup.
