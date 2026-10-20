# 0022: Workflow: branches and commit messages

**Status:** Accepted
**Date:** 2026-10-20

## Context

Bend's issues live in the Kendo tracker, and the repository is public on GitHub. The branching
model mirrors the one Kendo's own repository uses.

## Decision

- `main` is stable. It only moves through a release PR from `development`, merged as a merge
  commit (not squashed), so `main` stays a subset of `development`'s history. Release tags
  (`0.1.0`) go on `main`.
- `development` is the integration branch and GitHub's default branch. Issue branches are named
  `BD-NNNN-<slug>`, cut from `origin/development`, and PR'd back into it.
- Nobody commits directly on `main` or `development`.
- Small changes that don't warrant an issue (a typo, a README command, a dependency bump) use a
  keyless branch `<type>/<slug>`, where `<type>` is the commit type the branch carries (`fix`,
  `docs`, `chore`, `build`, ...). Branches made by a bot keep the bot's namespace
  (`dependabot/...`).
- Commit messages and PR titles reference the issue key (`... (BD-NNNN)`), so history and
  tracker stay linked. Keyless branches drop the suffix.

## Does not decide

- How a release is cut beyond "PR `development` into `main`, then tag". A `/release` skill is
  noted for the `Release hygiene` epic once `0.1.0` is near.
- A hotfix path from `main`. With `0.x` and one developer, fixes go through `development`.

## Rejected

- A fixed `fix/` prefix for every keyless branch: the commit type is a better fit and adds no
  second vocabulary.
