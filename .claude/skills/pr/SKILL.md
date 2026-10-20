---
name: pr
description: >
  Push the current branch and open (or refresh) a pull request against development on
  onestorm-net/bend. Requires `./gradlew build` to pass first, checks the outgoing diff and
  commit metadata for personal details (the repo is public), offers to run `/review-branch`
  when this session has no review for HEAD, gates bug branches on `bug-fix-verifier`'s verdict
  and embeds that verdict in the PR body, and moves the Kendo issue to In Review. The only skill
  that pushes. Use whenever the user wants to create a PR, open a pull request, submit their
  work for review, or is done with a feature/fix branch. Also triggers on "make a PR",
  "open PR", "create pull request", "submit for review", "I'm done with this branch", or
  "push and PR".
---

# Pull Request

Push the branch and open a PR against `development` on `onestorm-net/bend`, then move the linked Kendo
issue to In Review. This is the only skill that pushes.

## Workflow

### 1. Gather branch state

The base is always `development`. Run `git fetch origin development` first: every diff/log command below
compares against `origin/development`, and fetching is what keeps that ref current.

Then run these in parallel:
- `git status`, to check for uncommitted changes
- `git branch --show-current`, the branch name
- `git log origin/development..HEAD --oneline`, all commits on this branch
- `git diff origin/development...HEAD --stat`, the changed files summary

If on `development`, stop: there is nothing to PR. If on `main`, stop too: promoting `development` to `main` is a release PR, which this skill does not open. If there are uncommitted changes, ask the user
whether to `/commit` first.

Extract the issue key (`BD-\d{4}`) from the branch name. No key: the branch isn't issue-driven;
skip the Kendo steps below silently.

### 2. Build gate

```bash
./gradlew build
```

This runs compilation, every module's tests and the JaCoCo reports. It must pass before a PR is
opened. On failure, report the failing task and test(s) and stop; don't open the PR and don't
push. There is no CI on the repo, so this local run is the gate.

### 2b. Privacy check

`onestorm-net/bend` is public: whatever this push carries, in content or in commit metadata, is
published for good. GitHub's push protection blocks known token formats server-side; this step
catches what it can't recognise, personal details. Run both in parallel:

```bash
# Added lines: email addresses, absolute local paths, secret-shaped assignments
git diff origin/development...HEAD -U0 | grep -E '^\+[^+]' \
  | grep -nEo '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}|\b[A-Za-z]:[\\/][^ `)"]*|/(Users|home)/[^ `)"]*|(password|passwd|secret|token|api[_-]?key)[A-Za-z_]*["'\'']?\s*[:=]\s*["'\'']?[^ "'\''`]{8,}'

# Commit metadata: every author and committer address on the branch
git log origin/development..HEAD --format='%ae|%ce' | sort -u
```

**Allowed without asking:** `noreply@anthropic.com` (the `Co-Authored-By` trailer), addresses
`@onestorm.net`, `@users.noreply.github.com`, `example.com`/`example.org` placeholders, and
paths that are clearly placeholders in prose (`C:\path\to\...`).

**Anything else**, judge it in context. A hit that identifies a real person or machine (a
personal address, a `C:\Users\<name>` or `D:\...` path from someone's PC, a tenant or customer
name, a real credential) stops the PR:

> Privacy check found content that would be published:
> - `.claude/skills/foo/SKILL.md:42`: `C:\Users\<name>\...` (local path)
> - commit author `someone@gmail.com` on 3 commits
>
> Proceed with PR anyway? [y/N]

Default no. Fixing content is a normal commit. Fixing commit metadata means rewriting the
branch's history, so name it and let the user decide; never rewrite on your own.

### 3. Check the pre-PR review gate

`/pr` does not spawn the review finders itself. Which gate applies depends on the branch type.
Resolve `docs/issues/<branch-name>/` with
[`.claude/references/plan-directory.md`](/.claude/references/plan-directory.md); the file it holds
decides the type:

| Directory holds | Gate | Artifact | Missing artifact |
|---|---|---|---|
| `PLAN.md` | none required | optional `/review-branch` report | **ask** whether to run `/review-branch` |
| `BUG.md` (no `PLAN.md`) | `bug-fix-verifier` | `BUG.md` `## Verification` | **never prompt** for `/review-branch`, see below |
| no directory | none | none | skip the finder check |

#### Plan-driven branches

Look in **this conversation** for a `/review-branch` report whose `Reviewed against commit:`
matches `git rev-parse --short HEAD`. **Fresh** means it matches HEAD.

**Not found** → prompt:

> No review in this session for HEAD `<sha>`. Run `/review-branch` first? [y/N]

Default **no**. If accepted, invoke `/review-branch` and wait for it before continuing. If
declined, proceed. A missing review does not block the PR.

**Stale** (a report exists but its sha is not HEAD) → prompt:

> Review is stale: reviewed `<sha>`, now at `<sha>`.
>
> Re-run `/review-branch`? [y/N]

Default **no**. **Fresh** or declined → proceed. Do **not** embed the review in the PR body.
The pushed code is the record. Findings in the report do not block the PR; the session already
saw them.

#### Bug branches

`bug-fix-verifier`'s verdict in `BUG.md`'s `## Verification` section **is** the gate. It
substitutes for the finders, because a bug fix has no acceptance criteria to drift from, only
"does the defect still reproduce?". Embed that verdict in the PR body.

**The verdict blocks the PR** if it reads anything other than the exact string `PASS`,
including `FAIL` and `PARTIAL` (a verdict that
merely starts with "PASS" does not satisfy the check). Read only the **first** `**Verdict:**`
line under `## Verification`, before any `### ... (superseded ...)` subheading. Re-verification
passes leave older verdicts in place marked superseded; those don't gate the PR. Warn the user:

> Bug fix verification blocks the PR (per BUG.md § Verification):
> - Verdict: FAIL, defect still reproduces in `VarInt28FrameDecoderTest`
>
> Proceed with PR anyway? [y/N]

Default no. This check applies even when `/pr` is invoked directly, without going through
`/fix-bug` in the same session: the Verification section is the durable record, not the
conversation.

**No `**Verdict:**` line found at all** (`bug-fix-verifier` has never run). Treat this the same
as a blocking verdict, not as "nothing to check":

> No bug-fix verification found in BUG.md § Verification. Run `bug-fix-verifier` (via
> `/fix-bug`) before creating this PR.
>
> Proceed with PR anyway? [y/N]

Default no.

**Runtime integrity on a bug branch.** When `BUG.md` has a `### Runtime integrity` subsection
under `## Verification` (written by `runtime-integrity-reviewer`), do not block the PR on its
findings; add a pointer to it in the PR body's `## Bug Fix Verification` block.

**Never prompt for `/review-branch` on a bug branch.** If a `/review-branch` report *is* in this
session (the developer asked for one), its findings do not block the PR, and a stale or absent
report is not checked.

### 4. Push

```bash
git push -u origin HEAD
```

`-u` wires the branch to its own remote (`/newbranch` created it with `--no-track`). Then verify
with `git rev-parse --abbrev-ref @{upstream}` that the upstream is `origin/<branch-name>`, never
`origin/development`.

### 5. Create or update the pull request

Analyze ALL commits on the branch (not just the latest) to write:
- **Title**: same format as commits, `<type>(<scope>): <summary> (BD-NNNN)`, under 70
  characters: `feat(server-netty): add endpoint registry (BD-0012)`. A keyless branch drops the
  ` (BD-NNNN)` suffix.
- **Body**: Summary of what the PR accomplishes, the Kendo issue key (omit the `Kendo:` line on a
  keyless branch), plus Bug Fix Verification when that gate applies. **No Review Handoff
  block.** The pushed code is the record.

If a PR already exists for this branch (`gh pr list --head "$(git branch --show-current)"`),
splice the body using **Refreshing an existing PR body** below. Always splice `## Summary`.
Also splice `## Bug Fix Verification` when **this run** found a newer verifier verdict than the
body shows: replace the heading if it is already in the body, insert it if it is missing.

#### Refreshing an existing PR body

`gh pr edit --body` replaces the **whole** body. A Summary-only string wipes later `##`
sections. Always splice; never pass a partial body.

1. `old=$(gh pr view <n> --json body --jq .body)`
2. Note whether `$old` contains `## Bug Fix Verification`.
3. Build `$new` from `$old`. Replace from `## Summary` through (not including) the next
   `## ` heading, or through EOF if Summary is last.
4. When the verdict changed: replace from `## Bug Fix Verification` through the next `## `, or
   **insert** that heading after Summary if `$old` had none.
5. If `$old` had `## Bug Fix Verification` and `$new` does not, **abort**. A missing section
   may be added; a present one may not disappear unless step 4 replaced it.
6. `gh pr edit <n> --body "$new"`

Otherwise create it:

```bash
gh pr create --repo onestorm-net/bend --base development --title "<type>(<scope>): <summary> (BD-NNNN)" --body "$(cat <<'EOF'
## Summary
- Bullet points summarizing the changes

Kendo: BD-NNNN

## Bug Fix Verification
<!-- Bug branches only. Omit on plan-driven and no-directory branches. -->
- Verifier: 9/10 (PASS), defect no longer reproduces
- See `docs/issues/<branch-name>/BUG.md` § Verification.
- Runtime integrity: 1 finding, see BUG.md § Verification › Runtime integrity.  <!-- only when that subsection exists -->

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

### 6. Move the issue to In Review

When the branch has an issue key, move it to In Review (lane `3`) on the local `kendo` MCP
server:

1. Resolve the database id: `mcp__kendo__prepare-issue-context-tool` with
   `issue_key: "BD-NNNN"` (the key is not the id).
2. `mcp__kendo__update-issue-tool` with that `issue_id` and `lane_id: 3`.

Do not post a feedback comment on the issue. Skip this step if the issue is already in lane 3
or 4.

### 7. Return the PR URL to the user
