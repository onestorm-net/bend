# BD-0015: runtime-integrity-reviewer and precedent-reviewer don't load as agent types

**Date:** 2026-11-06
**Issue:** [BD-0015](https://onestorm.kendo.dev/projects/1/issues/BD-0015)
**Status:** Verified

## Problem
`/review-branch` spawns three finder agents in parallel, but in a Claude Code session in this
repo `runtime-integrity-reviewer` and `precedent-reviewer` come back "Agent type '<name>' not
found". Only `correctness-reviewer` runs, so every branch review loses two of its three lanes,
and `/fix-bug` loses the `runtime-integrity-reviewer` it spawns next to `bug-fix-verifier`. The
other agents in `.claude/agents/` load fine.

## Reproduction Steps

**Diagnosis evidence (3b):**
- Concrete file:line reference: `/.claude/agents/runtime-integrity-reviewer.md:3`
  ("...guards that got weaker: swallowed errors...") and `/.claude/agents/precedent-reviewer.md:3`
  ("...what is already written down: Bend's standing rulings...").
- Parsing the frontmatter (the text between the first two `---` lines) of every file in
  `.claude/agents/` with two YAML parsers, `yaml` 2.5.1 and `js-yaml` 4.1.0, on
  `origin/development`:

  ```
  bug-fix-verifier.md:           yaml OK   | js-yaml OK
  correctness-reviewer.md:       yaml OK   | js-yaml OK
  plan-reviewer.md:              yaml OK   | js-yaml OK
  precedent-reviewer.md:         yaml FAIL: Nested mappings are not allowed in compact mappings at line 2, column 14 | js-yaml FAIL: bad indentation of a mapping entry (2:66)
  runtime-integrity-reviewer.md: yaml FAIL: Nested mappings are not allowed in compact mappings at line 2, column 14 | js-yaml FAIL: bad indentation of a mapping entry (2:81)
  surface-reviewer.md:           yaml OK   | js-yaml OK
  ```

  All `.claude/skills/*/SKILL.md` frontmatter parses with both.
- Regression test: none ships with the fix. A check that all agent and skill frontmatter parses
  is out of scope for BD-0015 (see the issue's Scope). The repro is the parse above, re-run
  against HEAD with any YAML 1.2 parser.

## Root Cause
The `description:` value on line 3 of both files is a plain (unquoted) YAML scalar that contains
": " (colon followed by a space). In a plain scalar that sequence starts a new mapping entry, so
the frontmatter does not parse and Claude Code skips the agent. The colon came in with 6ea6416
(BD-0007), which replaced the em dash in both descriptions with ": "; the verbatim import cd381a3
had no colon there, so neither agent has loaded in bend since the adaptation.

## Chosen Approach
Wrap both `description:` values in double quotes, keeping the wording byte for byte (neither
value contains a `"` or a backslash, so nothing needs escaping).

## Fix
`/.claude/agents/runtime-integrity-reviewer.md` and `/.claude/agents/precedent-reviewer.md`:
the `description:` value on line 3 is now a double-quoted scalar. The ": " inside it is plain
text in a quoted scalar, so the frontmatter parses and `description` holds the same string as
before. No other line changes.

## Verification

**Verified:** 2026-11-06 by bug-fix-verifier
**Reviewed against commit:** a4c69d6 (plus uncommitted working-tree fix)
**Score:** 9 / 10
**Verdict:** PASS

### Reproduction

**Diagnosis evidence (3b):** verified that the cited evidence (`description:` on line 3 of `runtime-integrity-reviewer.md` and `precedent-reviewer.md`, unquoted ": ") matches the Fix location (the same line 3 in both files). No regression test ships (out of scope per BUG.md); the repro is the frontmatter parse. Result: **PASS**.

Before (origin/development versions, parsed from a temp dir):
```
precedent-reviewer.md: yaml FAIL: Nested mappings are not allowed in compact mappings at line 2, column 14 | js-yaml FAIL: bad indentation of a mapping entry (2:66)
runtime-integrity-reviewer.md: yaml FAIL: Nested mappings ... line 2, column 14 | js-yaml FAIL: bad indentation of a mapping entry (2:81)
```
After (working tree, all six agent files): every file `yaml OK | js-yaml OK`. `same.js` reports both descriptions identical to the original text. The parent session also saw both agent types listed in a fresh headless `claude -p` session.

### Regression scan

| File | Change | Note |
|------|--------|------|
| .claude/agents/precedent-reviewer.md:3 | OK | Only the description is wrapped in double quotes; no inner `"` or backslash. |
| .claude/agents/runtime-integrity-reviewer.md:3 | OK | Same, one line changed. |

Diff is 2 files, 1 line each; no other changes.

### Required fixes before PR
None.

### Runtime integrity

Run by a general-purpose agent following `/.claude/agents/runtime-integrity-reviewer.md`, because
the spawning session had started before the fix and so could not spawn that agent type.

## Findings

None.

## Checked

- Diff scope: two files, one line each (`.claude/agents/precedent-reviewer.md:3`,
  `.claude/agents/runtime-integrity-reviewer.md:3`); the plain `description:` scalar is now
  double-quoted with the wording unchanged. No Java, Gradle, protocol spec or wire code is touched.
- Security & cost surface: does not apply. No wire entry point, decoder, handler, endpoint
  register/unregister/dispatch or connection-state transition is touched; the guard set is the
  same at base and head.
- Runtime data-flow: does not apply. No transform, decoded peer input, or send/dispatch path.
- Contract & boundary integrity: the one boundary touched is the agent-frontmatter YAML contract
  with Claude Code. Both quoted values contain only the two delimiting `"` and no backslash, so
  the decoded `description` is byte-identical to the base text. The other four agent files have
  no `: ` or ` #` in a plain scalar on line 3, and every `SKILL.md` `description:` is a `|` or
  `>` block scalar, so no sibling has the same failure.
- Silent-failure and security focus: no catch, future, `ByteBuf`, executor, event-loop or
  disconnect path in the diff.
- Not re-verified: the reproduction, which belongs to bug-fix-verifier.

## Notes / Follow-ups
- Nothing checks that agent and skill frontmatter parses; a check is out of scope here (issue
  Scope, "Out") and would be a separate issue if wanted.
- The acceptance checks that need Claude Code itself (a new session lists both agent types,
  `/review-branch` starts all three lanes, `/fix-bug` starts `runtime-integrity-reviewer`) can
  only be confirmed in a session started after this fix is checked out.
