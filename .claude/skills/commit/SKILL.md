---
name: commit
description: >
  Create small, focused commits with Bend's message format
  `<type>(<scope>): <summary> (BD-NNNN)`. Keeps the branch's plan in step with the code it
  commits. Never pushes (only /pr pushes). Use whenever the user wants to commit, save
  progress, or says "commit" or "save my work".
---

# Commit

Create small, focused commits with proper messages. This skill does not push; `/pr` does.

## Workflow

### 1. Branch safety check

Run `git branch --show-current`. If it is `main` or `development`, **warn the user** and suggest creating a
feature branch first (`/newbranch`). Only proceed with committing once on an issue branch
(`BD-NNNN-<slug>`) or a keyless branch (`<type>/<slug>`, e.g. `fix/frame-decoder-typo`).

### 2. Gather context

Run in parallel:
- `git status` (never use the `-uall` flag)
- `git diff` and `git diff --staged`, for unstaged and staged changes
- `git log --oneline -10`, to see recent messages on this branch

Extract the issue key (`BD-\d{4}`) from the branch name; every commit message on this branch
carries it. A keyless `<type>/<slug>` branch has no key: its messages drop the ` (BD-NNNN)`
suffix, and nothing else changes.

### 3. Plan small commits

Group related changes into small, focused commits. Each commit should be a single logical unit:
- **Refactoring** separate from **new features**
- **Tests** separate from **implementation** when they stand alone (a test that pins the fix it
  ships with may go in the same commit)
- **Build config** (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`)
  separate from **code changes**
- **Spec changes** (`protocol/protocol-v1.md`) separate from implementation, unless the commit is
  exactly "implement this spec rule"
- **`.claude/`** and **`CLAUDE.md`** changes separate from code

**Keep the plan in step.** Resolve the branch's directory with
[`.claude/references/plan-directory.md`](../../references/plan-directory.md)
(`docs/issues/<branch-name>/`). When one exists and a commit's diff changes behaviour that
`PLAN.md`'s Approach or Scope, or a `DECISIONS.md` entry, describes, patch those files **in that
same commit**, before `git commit`, not later. Tests and renames leave the plan alone. A plan that
no longer matches its diff is what `precedent-reviewer` flags at `/review-branch`, and a stale
plan misleads every later session that resumes from it.

### 4. For each commit

a. Stage only the files for this logical change with `git add <specific files>`
   - Do NOT commit secrets or local-only files (`.claude/settings.local.json`, credentials, IDE
     state, `build/` output)
   - Prefer specific file paths over `git add -A` or `git add .`

b. Write the message in Bend's format:

```bash
git commit -m "$(cat <<'EOF'
<type>(<scope>): <summary> (BD-NNNN)

Optional body: the why, not the what.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

   - **type**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`.
   - **scope**: the module folder name (`protocol`, `client-api`, `client-netty`,
     `server-netty`), or `build`, `claude`, `docs` for non-module changes. A commit spanning
     modules is usually two commits; if it genuinely isn't, use the module that carries the
     behaviour change.
   - **summary**: imperative, lowercase start, no trailing period. Keep the whole first line
     under 72 characters including the `(BD-NNNN)` suffix.
   - Examples: `feat(client-api): add NamespacedId (BD-0001)`,
     `fix(protocol): reject zero-length frames (BD-0009)`,
     `chore(claude): adapt skills for Bend (BD-0007)`, and on a keyless branch
     `docs(docs): fix the Gradle command in the README`.
   - Always end with the `Co-Authored-By` trailer line above.

c. Repeat for each logical group of changes.

There are no git hooks in this repo. Never pass `--no-verify` anyway.

### 5. Report

Report the commit hashes and subjects. Remind the user that nothing was pushed; `/pr` pushes
and opens the pull request (and refreshes an open PR's Summary).
