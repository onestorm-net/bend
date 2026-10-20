# BD-0008: Clean up and split CLAUDE.md before it goes public

> Plan: [PLAN.md](PLAN.md) | Decisions: [DECISIONS.md](DECISIONS.md)

Documentation-only. There are no test classes, so `[RED]` means "run the check from PLAN.md's
Acceptance Criteria code block and see it fail (or have nothing to pass on)" and `[GREEN]` means
"write until it passes". Check numbers below refer to that code block. Nothing of the old
CLAUDE.md is ever committed (D7); until task 1.3 the old file stays in place, untracked.

### Phase 1: Split CLAUDE.md (5 tasks)

**Goal:** A short public CLAUDE.md, four module CLAUDE.md files and 24 ADRs replace the
untracked 740-line CLAUDE.md, with every decision kept and every citation still resolving.

**Phase Context:**

- Why a checkpoint after 1.1: the binding rule (D3) and the mapping are what keep 24 ADRs from
  over-steering later sessions; the developer reviews them before the ADRs are written on top.
- Why NOT one big commit: D7 only requires that `.gitignore` and the first tracked CLAUDE.md
  land together (task 1.3). ADRs and the citation sweep are separately reviewable commits.

- [x] **1.1** Backup, local references, decision index and mapping table
    - **Context:**
        - **Why:** The old file is untracked with no history, so it is backed up before anything can overwrite it (D9). The mapping table is the only guard against losing a decision (AC 2), so it is built before any ADR is written (PLAN step 3).
        - **Architecture:** `docs/decisions/README.md` states the three statuses and that only the Decision section of an `Accepted` ADR binds (D3); new decisions go in `docs/issues/<branch>/DECISIONS.md`, a lasting rule gets a CLAUDE.md line.
        - **Key refs:** old `CLAUDE.md` headings and bold bullets (`grep -nE '^#{1,4} |^\s*- \*\*' CLAUDE.md`, 68 lines on 2026-10-20); PLAN.md Approach step 3 (known dropped rows) and step 4 (ADR table, the row source for each heading); `.gitignore` `### Claude Code ###` block.
        - **Watch out:**
            - The backup location and the local-only facts go only into `.claude/local-references.md`, never into a committed file (D5).
            - `.claude/local-references.md` must be ignored before it is created, so it can't be staged by accident.
            - The mapping table lives in the working notes for the PR description; local-only facts appear there as "dropped: local reference", never quoted.
    - **Scope:** Backup outside the repo; `.gitignore` gains `.claude/local-references.md` (the `CLAUDE.md` line stays until 1.3); `.claude/local-references.md`; `docs/decisions/README.md` with the index table for 0001 to 0024; the mapping table (old heading or bold bullet to destination) kept in this branch's planning folder as `MAPPING.md`.
    - **Acceptance Criteria:** AC 2 (partial: table built, checked off in 1.5), AC 4 (partial: README and index), AC 9 (partial: ignore rule)
    - **Touches:** `.gitignore`, `.claude/local-references.md` (local), `docs/decisions/README.md`, `docs/issues/BD-0008-split-claude-md/MAPPING.md`
    - **Action items:**
        - [RED] `git check-ignore .claude/local-references.md` prints nothing (not ignored yet)
        - [RED] Check 4 fails: `docs/decisions/` doesn't exist
        - [GREEN] Copy the old `CLAUDE.md` outside the working tree; add `.claude/local-references.md` to `.gitignore`; create it with the backup location and the old file's local-only references
        - [GREEN] Write `MAPPING.md`: one row per line of the heading/bold-bullet grep, each with its ADR, module/root CLAUDE.md line, "dropped: status" plus Kendo issue or epic, or "dropped: local reference"
        - [GREEN] Write `docs/decisions/README.md`
    - **Verify before complete:**
        - [ ] `git check-ignore .claude/local-references.md` prints the path; `git status --short` doesn't list it
        - [ ] The backup opens outside the repo and matches the old file (`cmp`)
        - [ ] `MAPPING.md` row count equals the heading/bold-bullet grep's line count, with no empty destination
        - [ ] Check 9's pattern over `MAPPING.md` and `docs/decisions/README.md` finds nothing
        - [ ] README index lists 0001 to 0024 with one status each (D11)
    - **Success:** Old file safe outside the repo, every decision has a destination on paper, and the binding rule is written down. **Checkpoint: developer reviews README.md and MAPPING.md before 1.2.**
    - **Completed:** 2026-10-20
    - **Learnings:**
        - The heading/bold-bullet grep gives 68 lines, not the 57 a reviewer counted; MAPPING.md is diffed against the grep's line numbers, so the count is mechanical.
        - Several decisions live in plain bullets (version bump, root build layout, 0.x, Javadoc scope, docs site, supported ids); they get `+` rows so the 68-row diff stays exact.
        - A local Velocity clone exists and is the codec's upstream; it went into `local-references.md` too.
    - **Key Changes:**
        - Modified `.gitignore`: ignores `.claude/local-references.md` (the `CLAUDE.md` line stays until 1.3)
        - Created `docs/decisions/README.md`: status meanings, binding rule, shape, index of 0001 to 0024
        - Created `docs/issues/BD-0008-split-claude-md/MAPPING.md`: 68 grep rows plus plain-bullet rows
        - Local only: backup outside the repo and `.claude/local-references.md`
    - **Notes:** The developer asked to run `/next` and `/commit` straight through, stopping only for decisions, so the checkpoint was not held as a pause; README.md and MAPPING.md are in the 1.1 commit for review at any time.

- [x] **1.2** ADRs 0001 to 0024
    - **Context:**
        - **Why:** Keeps every decision's reasoning and alternatives once the root file shrinks (D1).
        - **Architecture:** Shape per PLAN step 4: `# NNNN: <title>`, `**Status:**`, optional `**Date:**`, `## Context`, `## Decision`, `## Does not decide`, `## Rejected`. `Deferred`/`Open` ADRs open their Decision with "This ADR binds nothing in 0.1." ADRs 0001 to 0007 cite `protocol/protocol-v1.md` sections for rule text and keep only the reasoning. A reversal is one ADR (D2, ADR 0018).
        - **Key refs:** PLAN.md step 4 table (title, status, source per ADR); `MAPPING.md`; `protocol/src/main/java/net/onestorm/bend/protocol/exception/ProtocolException.java:8-13` for ADR 0011.
        - **Watch out:**
            - Move decisions, don't reword them into new rulings; "Does not decide" names what stays open instead of inventing direction.
            - Record the code as it is: `ProtocolException`'s debug accessors are Lombok-generated from the static `debugStackTraces` field.
            - No local-only facts: ADR 0012/0018 come from sections that mention the pre-migration copy; ADR 0022 from text naming another repo.
            - Forward-looking notes in an `Accepted` ADR go under "Does not decide" (the `/release` skill note in 0022; the `server-api` re-add in 0016).
    - **Scope:** 24 files `docs/decisions/NNNN-<slug>.md`, ticking each off in `MAPPING.md`.
    - **Acceptance Criteria:** AC 3, AC 4 (partial: file count), AC 8, AC 9 (partial: content), AC 10 (partial)
    - **Touches:** `docs/decisions/0001-*.md` to `docs/decisions/0024-*.md`, `docs/issues/BD-0008-split-claude-md/MAPPING.md`
    - **Action items:**
        - [RED] Checks 3 and 8 have no files to pass on; check 4 prints `0` and `24`
        - [GREEN] Write ADRs 0001 to 0024 in order, ticking `MAPPING.md` rows
    - **Verify before complete:**
        - [ ] Check 3 prints nothing (all five sections; Deferred/Open say "binds nothing")
        - [ ] Check 4 prints `24` twice
        - [ ] Check 8 prints nothing
        - [ ] Moved, not reworded: spot-read 0001, 0011, 0016, 0018 against the backup
        - [ ] Lombok note present in 0011
        - [ ] Check 9's pattern over `docs/decisions/` finds nothing
        - [ ] Forward-looking notes sit under "Does not decide" in 0016 and 0022
        - [ ] Check 10 over `docs/decisions/` prints nothing
    - **Success:** 24 ADRs, each with one status and the five sections, every ADR row in `MAPPING.md` ticked.
    - **Completed:** 2026-10-20
    - **Learnings:**
        - Moving decisions without widening them takes active checking: a first draft of 0004 said "every reader that allocates" must validate, where the old file only ruled on the two named readers. Narrowed back, with the general case under Does not decide.
        - Machine-specific facts hide in Context prose too ("system-wide Gradle 8.8" in 0014); reworded to the general fact.
        - Check 8 caught 0004 missing its spec link; the arrays are defined in the Available/Supported Endpoints packet sections.
        - `ProtocolException` passes `enableSuppression = false` as well as `writableStackTrace = debugStackTraces`; 0011 records both.
    - **Key Changes:**
        - Created `docs/decisions/0001-*.md` to `docs/decisions/0024-*.md`: 21 Accepted, 2 Deferred (0017, 0024), 1 Open (0015)
        - Modified `MAPPING.md`: 58 rows ticked; 34 rows wait on root or module CLAUDE.md lines (task 1.3)

- [x] **1.3** Module CLAUDE.md files, root CLAUDE.md, `.gitignore`
    - **Context:**
        - **Why:** The root file becomes short, current and public; module rules load only when working in that module (D4).
        - **Architecture:** Root section order per PLAN step 6, keeping the headings `.claude/` cites (D6): `Protocol architecture`, `API design decisions`, `Testing / coverage`, `Module naming convention`, `Module set`, `Build tooling`, `Documentation scope`, `Workflow`. One line per rule, linking its ADR. Includes the binding rule (D3) and how to set up and use `.claude/local-references.md` (D5).
        - **Key refs:** PLAN.md steps 5 and 6; `MAPPING.md` rows pointing at a module or root line.
        - **Watch out:**
            - D7: the `CLAUDE.md` line leaves `.gitignore` in the same commit as the rewritten file; the old content is never staged.
            - The three empty modules carry only decided rules: no status, "planned" or future-direction lines.
            - No local-only facts in any CLAUDE.md; the `local-references.md` section describes the format, not this machine's entries.
    - **Scope:** `protocol/CLAUDE.md`, `client-api/CLAUDE.md`, `client-netty/CLAUDE.md`, `server-netty/CLAUDE.md`, root `CLAUDE.md` rewritten in place, `.gitignore` drops `CLAUDE.md`.
    - **Acceptance Criteria:** AC 1, AC 5, AC 9 (partial: tracked), AC 10 (partial)
    - **Touches:** `CLAUDE.md`, `protocol/CLAUDE.md`, `client-api/CLAUDE.md`, `client-netty/CLAUDE.md`, `server-netty/CLAUDE.md`, `.gitignore`
    - **Action items:**
        - [RED] Check 1 prints the old `## Status`, `## Frame decoder`, `## In-flight work`, `## Versioning` lines; check 5's `ls` fails
        - [GREEN] Write the four module files
        - [GREEN] Overwrite root `CLAUDE.md` (backup exists from 1.1); remove `CLAUDE.md` from `.gitignore`; stage both together
    - **Verify before complete:**
        - [ ] Check 1 prints nothing
        - [ ] Check 5: all four files exist and each links `docs/decisions/`
        - [ ] D7: `git diff --cached` shows the `.gitignore` change and the new `CLAUDE.md` in the same staged set, no old content
        - [ ] Empty modules: read-through finds no status or future-direction line
        - [ ] Check 9 over the staged files finds nothing
        - [ ] `git ls-files --error-unmatch CLAUDE.md` succeeds after the commit
        - [ ] Check 10 over the five CLAUDE.md files prints nothing
    - **Success:** Root CLAUDE.md is short and tracked; every module has its rules; module and root rows in `MAPPING.md` ticked.
    - **Completed:** 2026-10-20
    - **Learnings:**
        - The same widening risk as in 1.2 shows up in one-line rules: a first draft of `protocol/CLAUDE.md` turned ADR 0004 into "validate every peer-supplied array length", and stated "100% coverage" as if it were a rule. Both narrowed back to what the ADRs decide.
        - Root CLAUDE.md is 124 lines (was 740). Git sees it as a new file, so the staged diff is proof that no old content went in (D7).
        - The test-convention line (JUnit Jupiter, AssertJ, `method_condition_expected`) has no ADR: it is codebase convention, already enforced by the skills.
    - **Key Changes:**
        - Rewrote `CLAUDE.md` (now tracked): modules, where things live, binding rule, rule groups under the cited headings, local references, writing style
        - Created `protocol/CLAUDE.md`, `client-api/CLAUDE.md`, `client-netty/CLAUDE.md`, `server-netty/CLAUDE.md`
        - Modified `.gitignore`: no longer ignores `CLAUDE.md`
        - `MAPPING.md`: all 92 rows ticked

- [x] **1.4** Citation sweep in `.claude/` and build scripts
    - **Context:**
        - **Why:** Skills and reviewer agents cite CLAUDE.md sections and describe it as holding history and declined alternatives; after 1.3 that content lives in ADRs (D6, D8, D10).
        - **Architecture:** Follow PLAN.md step 7's five groups: read `docs/decisions/` and respect Status; role descriptions; the surface-reviewer row table with `Accepted` ADR numbers only; history citations repointed; kept headings; plus the three `build.gradle.kts` comments.
        - **Key refs:** PLAN.md step 7 (every site with its line number as of 2026-10-20); `git grep -n 'CLAUDE\.md' -- ':!docs/issues'`.
        - **Watch out:**
            - Line numbers drift as a file is edited: edit each file bottom-up, or re-grep it.
            - `plan-reviewer.md:54` takes next consumers from open 0.1 issues, never from ADR 0017 (D8).
            - Surface-reviewer rows 3 and 7 cite `protocol-v1.md` and 0016/0019, not 0017 (D10); row 6 takes its enforcement ladder from `surface-questions.md`, not ADR 0015.
            - A heading that survives while its content moved (`§ Module set (in-flight packets during disable)`) is repointed, not kept.
            - `server-netty/build.gradle.kts:1` also loses its em dash.
    - **Scope:** Every hit of check 6 outside `docs/issues/`.
    - **Acceptance Criteria:** AC 6, AC 7, AC 10 (partial)
    - **Touches:** `.claude/agents/plan-reviewer.md`, `.claude/agents/precedent-reviewer.md`, `.claude/agents/surface-reviewer.md`, `.claude/skills/review-branch/references/corpus/recorded-rulings.md`, `.claude/skills/review-branch/SKILL.md`, `.claude/skills/review-branch/references/finder-base.md`, `.claude/skills/plan-feature/SKILL.md`, `.claude/skills/plan-feature/references/plan-template.md`, `.claude/skills/plan-feature/references/anti-patterns.md`, `.claude/skills/fix-bug/SKILL.md`, `.claude/skills/fix-bug/references/repro-paths.md`, `.claude/skills/kendo/SKILL.md`, `.claude/skills/kendo/references/issue-templates.md`, `client-api/build.gradle.kts`, `client-netty/build.gradle.kts`, `server-netty/build.gradle.kts`
    - **Action items:**
        - [RED] Check 7 lists none of the four files; check 6 shows citations to `§ Frame decoder`, `§ In-flight work (client)`, `§ Status` and "Deferred to 0.2+"
        - [GREEN] Apply PLAN.md step 7 file by file, bottom-up
    - **Verify before complete:**
        - [ ] Check 6: every hit is a kept heading that exists in a CLAUDE.md file, or names an existing `docs/decisions/` file
        - [ ] Check 7 lists all four files for both patterns
        - [ ] D8: `plan-reviewer.md` no longer mentions "Deferred to 0.2+" or ADR 0017 as a consumer source
        - [ ] D10 and row 6: the surface-reviewer table cites no `Deferred`/`Open` ADR
        - [ ] Moved-content headings: no citation of `§ Module set (` remains
        - [ ] Check 10 over `.claude/` and `*/build.gradle.kts` prints nothing
    - **Success:** Every CLAUDE.md citation in the repo resolves; the reviewer agents read `docs/decisions/` and honour Status.
    - **Completed:** 2026-10-20
    - **Learnings:**
        - All three `build.gradle.kts` files contained only their status comment, and all three had an em dash (the plan only named `server-netty`'s). Deleting the lines left empty files, so `sed '1i'` inserted nothing; a shell redirect was blocked as an overwrite, so the pointer comments were written with the file tools.
        - A citation-check regex over `§ <heading>` picks up trailing prose; the real stray was `surface-questions.md:90` citing `§ Testing` without `/ coverage`.
        - Line numbers had already drifted in `plan-feature/SKILL.md` after its first edit; editing by exact text, not line number, avoided it.
    - **Key Changes:**
        - Reviewer agents (`plan-reviewer`, `precedent-reviewer`, `surface-reviewer`) and `recorded-rulings.md` read `docs/decisions/`, treat only `Accepted` Decision sections as rulings, and cite ADR numbers; surface-reviewer's row table names ADRs and excludes 0017 (D10)
        - `plan-reviewer.md`: next consumers come from open 0.1 issues, never a Deferred ADR (D8)
        - `plan-feature` SKILL and references, `fix-bug`, `kendo`, `review-branch` SKILL and `finder-base.md`: history and role citations repointed to ADRs
        - `client-api`, `client-netty`, `server-netty` `build.gradle.kts`: status comment replaced by a pointer to the module's CLAUDE.md

- [x] **1.5** Final verification
    - **Verify:**
        - [ ] Checks 1, 3, 4, 5, 6, 7, 8 and 10 from PLAN.md's code block all pass on the full tree
        - [ ] Check 9 over the index (`git grep --cached`) finds nothing; `git check-ignore .claude/local-references.md` matches
        - [ ] AC 2: every `MAPPING.md` row ticked, re-checked against the backup's headings and bold bullets
        - [ ] The PR description (built from `MAPPING.md`) passes check 9's pattern before `/pr` opens it
        - [ ] `./gradlew build` passes (the build-script comment edits must not break it)
        - [ ] JaCoCo gap check: N/A, no Java source changed (`git diff --stat development... -- '*/src/'` is empty)
    - **Success:** All ten acceptance criteria verified, build green, nothing local-only staged or in the PR text.
    - **Completed:** 2026-10-20
    - **Learnings:**
        - The plan's src check used `development...`; this checkout only has `origin/development`, so use the remote ref.
        - Results on the committed tree: checks 1, 3, 8, 10 print nothing; check 4 prints 24 and 24; check 5 finds ADR links in all four module files; check 7 lists all four reviewer files for both patterns; check 9's pattern finds nothing in `HEAD`, `CLAUDE.md` is tracked and `local-references.md` ignored and untracked; 0 of 92 `MAPPING.md` rows open; 0 files under `*/src/` changed; `./gradlew build` passes.
        - The PR description check (check 9's pattern over the body) runs at `/pr`, when the body exists.

## Coverage Check

- [x] Every "Files to modify" appears in at least one task's Touches
- [x] Every "Files to create" appears in at least one task's Touches
- [x] Every disconnect condition / error path from the plan is in a task's "Watch out" or action items (none in this plan; privacy and drift risks are in Watch out instead)
- [x] Every spec change is in the same task as the code that implements it (no spec change)
- [x] Every verification step from the plan is in the final verification task
- [x] Every PLAN.md acceptance criterion is mapped to at least one task's `**Acceptance Criteria:**` line

## Review Notes

**Reviewed:** 2026-10-20

### Coverage
- **Acceptance criteria:** 10/10 covered (AC 1: 1.3; AC 2: 1.1, 1.5; AC 3: 1.2; AC 4: 1.1, 1.2; AC 5: 1.3; AC 6: 1.4; AC 7: 1.4; AC 8: 1.2; AC 9: 1.1, 1.2, 1.3, 1.5; AC 10: 1.2, 1.3, 1.4)
- **Planned files:** 33/33 appear in some task's `**Touches:**` (root and 4 module CLAUDE.md, `.gitignore`, README plus 24 ADRs counted as their glob, `local-references.md`, 13 `.claude/` files with edits, 3 build scripts); generic-mention files in step 7 are checked, not edited

### Gaps
- *(None)*

### Scope Drift
- `MAPPING.md` (task 1.1) is a file the plan doesn't name: PLAN step 3 requires the table and says it goes into the PR description, but not where it lives meanwhile. Kept in the planning folder so it survives across sessions; it carries no local-only facts by the same rule. Plan unchanged; flagged for the developer.
