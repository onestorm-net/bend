# Claude Code setup for Bend

Project-local skills (`skills/`) and agents (`agents/`) for working on Bend with
[Claude Code](https://claude.com/claude-code).

## Credits

These skills and agents are adapted from
[script-development/claude](https://github.com/script-development/claude), the
Claude Code skill catalog of Script Development. Many thanks to its authors.

- Copied from the `plugins/core-skills` and `plugins/kendo-pm` plugins at commit
  [`f0c3fb8`](https://github.com/script-development/claude/commit/f0c3fb8).
- The first import commit on this repo contains them unmodified; every later
  change is Bend's own adaptation (Java 25 / Gradle, Windows, Kendo project
  `BD`, `docs/issues/<branch>/` planning folders, no CI, no worktrees).
- Synced by hand up to
  [`92f98ad`](https://github.com/script-development/claude/commit/92f98ad):
  the kendo-mcp tool surface, issue templates ending in `## What done looks
  like` (without upstream's `Browser look:` line, Bend has no UI), and
  `verify-citations.sh` failing closed on empty or unreadable input.
- To pick up upstream improvements, diff upstream from `92f98ad` and port the
  changes by hand. This is a fork, not a vendored dependency.

## Workflow

| Step | Skill |
|---|---|
| Pick up an issue, create and link its branch | `/prepare-issue` (or `/newbranch`) |
| Plan a feature into `docs/issues/<branch>/PLAN.md` + `DECISIONS.md` | `/plan-feature` |
| Break the plan into `TASKS.md` | `/task-writer` |
| Implement the next task | `/next` |
| Investigate a bug into `docs/issues/<branch>/BUG.md` | `/fix-bug` |
| Review the branch before a PR | `/review-branch` |
| Commit (local only) | `/commit` |
| Push and open the PR | `/pr` |
| Merge `development` into the branch | `/catchup` |
| Talk to the Kendo tracker | `/kendo` |

## Branches

- `main` is stable and only moves through a release PR from `development` (merge commit; tags
  go on `main`).
- `development` is the integration branch and the GitHub default; every PR targets it.
- Issue branches: `BD-NNNN-<slug>`, from `origin/development`.
- Small changes with no issue: `<type>/<slug>`, where `<type>` is the commit type
  (`fix/frame-decoder-typo`, `docs/readme-gradle-command`, `chore/bump-netty`). See `/newbranch`.

`settings.local.json` holds per-user permissions and is not tracked.
