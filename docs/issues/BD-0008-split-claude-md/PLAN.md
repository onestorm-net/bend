# BD-0008-split-claude-md: Clean up and split CLAUDE.md before it goes public

**Date:** 2026-10-20
**Status:** Approved
**Issue:** [BD-0008](https://onestorm.kendo.dev/projects/1/issues/BD-0008)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0008-split-claude-md/DECISIONS.md)

## Goal
Anyone who clones Bend, and every Claude session working in it, gets a short, current, public
CLAUDE.md with the hard rules, and can follow each rule to the reasoning behind it, without
recorded history or ideas being mistaken for requirements.

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| ADR granularity | One ADR per decision, 24 in `docs/decisions/NNNN-<slug>.md`, each with exactly one status (D1, D11) | Issue BD-0008 "Agreed direction": ADRs for the decisions that already exist |
| Reversed decisions | One ADR for the rule in force; the reversed option under Rejected with dates (D2) | Old CLAUDE.md § In-flight work (client): ServiceLoader decided 2026-09-17, reversed 2026-10-04 |
| What binds | ADR Status `Accepted` / `Deferred` / `Open`; only the Decision section of an `Accepted` ADR binds; every ADR has a "Does not decide" section (D3) | Developer requirement in planning; no precedent in the repo, deliberate addition |
| ADR shape | Status / Context / Decision / Does not decide / Rejected | `.claude/skills/plan-feature/references/decisions-template.md` (Context / Decision / Rejected), plus "Does not decide" |
| Module files | `protocol`, `client-api`, `client-netty`, `server-netty` each get a CLAUDE.md with decided rules only; the three empty modules stay a few lines (D4) | `.claude/agents/plan-reviewer.md:28` already discovers module CLAUDE.md files with `find . -name CLAUDE.md` |
| Local-only facts | Gitignored `.claude/local-references.md`; committed files carry no local paths, local folder names or memory pointers (D5) | `.gitignore` `### Claude Code ###` block already ignores `.claude/settings.local.json` |
| Citation stability | Root CLAUDE.md keeps the headings the skills cite where the rule still lives there; citations to moved content repoint to the ADR (D6) | ~30 `§`-style citations in `.claude/` (Approach step 7) |
| Reviewers' future-consumer source | Open issues in the 0.1 epics, not Deferred ADR 0017 (D8) | `.claude/agents/plan-reviewer.md:54` |
| Publishing | Backup outside the repo first (D9); `.gitignore` change and first tracked CLAUDE.md in one commit, rewritten content only (D7) | `/pr` step 2b privacy check (`.claude/skills/pr/SKILL.md:49`) |

**Layer compliance:** N/A: no Java classes are added or changed.

## Scope
- **In scope:**
  - Rewrite root `CLAUDE.md` (Approach step 6).
  - New `protocol/CLAUDE.md`, `client-api/CLAUDE.md`, `client-netty/CLAUDE.md`, `server-netty/CLAUDE.md`.
  - New `docs/decisions/README.md` and ADRs 0001 to 0024.
  - Write every Markdown link to a repo file from the root as `/path` (D12), including the skills in `.claude/` and the templates.
  - Repoint the CLAUDE.md citations in `client-api/build.gradle.kts:2`, `client-netty/build.gradle.kts:2` and `server-netty/build.gradle.kts:1` (comments only; the last also carries an em dash).
  - `.gitignore`: drop `CLAUDE.md`, add `.claude/local-references.md`.
  - Update every CLAUDE.md citation and role description in `.claude/` (Approach step 7).
  - The developer's own `.claude/local-references.md` (local, never committed), including the old file's backup location.
- **Out of scope:**
  - Changing any decision (an ADR records a ruling as it stands, including `Open` and `Deferred` ones).
  - Creating Kendo issues for status that moves out (Approach step 3 shows where Kendo already holds it).
  - A root README, and a committed template for `local-references.md`.
  - Rewriting `protocol/protocol-v1.md` (ADRs link to it; the spec stays the source of truth for wire rules).

## Approach

Nothing of the old file is ever committed (D7). Until step 6, the old `CLAUDE.md` stays in
place, untracked.

1. **Back up** the old `CLAUDE.md` outside the working tree and record the location in `.claude/local-references.md` (D9). Create `.claude/local-references.md` now, with the local-only references the old file mentions.
2. **`docs/decisions/README.md`**: status meanings (`Accepted` binds; `Deferred` records direction and binds nothing; `Open` records an unresolved question and binds nothing); the rule that only the Decision section of an `Accepted` ADR binds and nothing is designed toward a `Deferred` ADR; that new decisions go in the issue's `docs/issues/<branch>/DECISIONS.md` (BD-0008's agreed direction) and a lasting rule gets a CLAUDE.md line; the index table (number, title, status).
3. **Mapping table**, filled in before any ADR is written and checked off as each lands. Every `##`/`###` heading and bold decision bullet of the old file gets a row: an ADR, a module CLAUDE.md line, a root CLAUDE.md line, or "dropped: status" with the Kendo issue or epic that holds it. Known dropped rows: § Status (protocol design done, Gradle migration done, client modules reset, server not started; tracked by epics 2 to 6 and BD-0001 to BD-0006); § Workflow "Tracker is live" (now in `.claude/skills/kendo/SKILL.md`); § In-flight work EndpointCodec "known issue" status (BD-0002). § Build tooling "Target repo / Issue tracking" is not dropped: it becomes root CLAUDE.md's "where things live" line. Local-only facts get the row "dropped: local reference" and are never quoted. The table goes into the PR description, which AC 9's pattern is also run over before `/pr` opens it.
4. **ADR file shape**, used by every ADR: `# NNNN: <title>`, `**Status:**`, `**Date:**` (only where the old CLAUDE.md records one), `## Context`, `## Decision`, `## Does not decide`, `## Rejected` (`None recorded.` when the old file lists none). `Deferred` and `Open` ADRs open their Decision section with "This ADR binds nothing in 0.1." No em dashes. ADRs 0001 to 0007 cite the `protocol-v1.md` section for rule text and keep only the reasoning.

   | ADR | Title | Status | From old CLAUDE.md |
   |---|---|---|---|
   | 0001 | Frame length prefix is a 28-bit VarInt with a separate configurable cap | Accepted | § Frame decoder: width bug, `VarInt21` to `VarInt28` rename, `readRawVarIntSmallBuf` reset, `maximumFrameLength` constructor and clamp, no no-arg constructor; Length field narrower than general VarInt; general VarInt byte order spelled out |
   | 0002 | NUL padding before a frame is skipped; more than 16 disconnects | Accepted | § Frame decoder: `FIND_NON_NUL` |
   | 0003 | Zero-length frames are rejected; canonical VarInt encoding is not required | Accepted | § Frame decoder: zero-length frame, canonical encoding declined; Packet ID one byte; fields consume exactly Length, `Endpoint Message` consumes the remainder |
   | 0004 | Array lengths are validated against readable bytes before allocating | Accepted | § Frame decoder: array pre-allocation fix (`ProtocolUtil#validateArrayLength`, `AvailableEndpointsPacket#readEndpointArray`) |
   | 0005 | Endpoint flags and ids are validated on construction and decode | Accepted | § Frame decoder: `Endpoint` direction bit and id rules |
   | 0006 | Client-side disconnect conditions, wrong-direction messages, keep-alive echo check | Accepted | § Frame decoder: spec gaps closed in the 2026-09-16 audit |
   | 0007 | One `Timeout` disconnect type covers every timeout | Accepted | § Frame decoder: `Disconnect` `Type` resolution (2026-09-17) |
   | 0008 | Two protocol layers, two send paths | Accepted | § Protocol architecture: two layers |
   | 0009 | Unreachable defensive checks are deleted, not kept | Accepted | § Testing / coverage last bullet; § Frame decoder: `length < 0` and `isReadable()` removals as Context |
   | 0010 | Coverage is reported as XML and CSV, with no gate yet | Accepted | § Testing / coverage: JaCoCo setup, no HTML, no verification gate |
   | 0011 | `ProtocolException` for wire input, without stack traces by default | Accepted | § API design decisions: `ProtocolException` (flat class, 4-arg `Throwable` constructor, static `ProtocolException#debugStackTraces` with Lombok accessors, `IllegalArgumentException` for setup misuse) |
   | 0012 | No Netty types in public API modules | Accepted | § API design decisions: `ByteBuf` vs `java.nio.ByteBuffer`; § In-flight work: `EndpointCodec` lands on `ByteBuffer` in the client rework, not patched in place |
   | 0013 | API modules get the plain name, implementations a transport suffix | Accepted | § Module naming convention |
   | 0014 | Gradle build layout | Accepted | § Build tooling: Kotlin DSL, version catalog, folder vs project names, `group net.onestorm`, root `plugins { }` block, `apply<PluginType>()` inside `subprojects`, `VersionCatalogsExtension` over the generated accessor, test suites, wrapper 9.7.1 |
   | 0015 | Style enforcement tooling | Open | § Code formatting / style enforcement: Spotless and Checkstyle removed, requirements for a future tool, `final` on locals declined |
   | 0016 | The 0.1 module set: no `server-api`, registration in `server-netty` | Accepted | § Module set: For 0.1, the dividing line (runtime endpoint add/remove is core protocol, ownership tracking is module system), forward-compatibility note, don't half-build loading in 0.1, `server-api` re-add naming; § Status: runtime add/remove is a core protocol feature |
   | 0017 | Module system | Deferred | § Module set: Deferred to 0.2+ (module interface, ownership tracking, loading, hot-swap, enable/disable on live connections, D10) |
   | 0018 | A `Client` is built by its implementation's builder; `client-api` depends on nothing | Accepted | § In-flight work (client): builder decision, ServiceLoader under Rejected (D2), `client-api` depends on nothing (not `protocol`, not Netty) and owns its value types |
   | 0019 | Supported endpoint ids are resent as a full snapshot | Accepted | § In-flight work (client): supported ids and triggers |
   | 0020 | The first release is 0.x | Accepted | § Versioning / release plan; `0.1.0-SNAPSHOT` from § Build tooling |
   | 0021 | Javadoc covers public contract modules only | Accepted | § Documentation scope: Javadoc scope and the per-module gut-check |
   | 0022 | Workflow: branching model, keyless branches, issue keys in commits | Accepted | § Workflow going forward: branching model and keyless branches (2026-10-20), commit messages reference issue keys; the "build a `/release` skill near 0.1.0" note goes under Does not decide |
   | 0023 | Epic structure for 0.1 | Accepted | § Workflow going forward: epic breakdown, no `Protocol` epic, sequencing `client-api` then `client-netty` then `server-netty` then `Release hygiene` and why, no `server-api` epic before 0.1.0 |
   | 0024 | Docs site | Deferred | § Documentation scope: docs site and versioned Javadoc hosting (D11) |

5. **Module CLAUDE.md files** (rules only, each rule linking its ADR):
   - `protocol/CLAUDE.md`: codec lineage (adapted from Velocity), frame rules (0001 to 0004), `Endpoint` validation (0005), `ProtocolException` vs `IllegalArgumentException` (0011), deleting unreachable checks and the coverage reports (0009, 0010).
   - `client-api/CLAUDE.md`: depends on nothing; owns its value types (0018); no Netty types, `java.nio.ByteBuffer` at the boundary (0012); no `connect()` and no settings on `Client` (0018).
   - `client-netty/CLAUDE.md`: built-in packets never go through the public send path (0008); `ByteBuf` converted at the boundary (0012); concrete client package-private, builder returns `Client` (0018).
   - `server-netty/CLAUDE.md`: registration lives here in 0.1 with Netty-agnostic signatures (0016).
6. **Root `CLAUDE.md` and `.gitignore`**, in one commit (D7). Sections, in order: what Bend is; module map; build (`./gradlew` only, JDK 25, wrapper 9.7.1); where things live (spec, `docs/decisions/`, `docs/issues/<branch>/`, Kendo for status); rule groups under the headings `.claude/` cites, one line per rule with its ADR link: `Protocol architecture`, `API design decisions`, `Testing / coverage`, `Module naming convention`, `Module set`, `Build tooling`, `Documentation scope`, `Workflow`; how recorded decisions bind (D3); `local-references.md` setup and use (D5); writing style (no em dashes).
7. **Citation updates**, every hit of `git grep -n 'CLAUDE\.md' -- ':!docs/issues'`. Line numbers below are from 2026-10-20; edit each file bottom-up, or re-grep it, so earlier edits don't shift the targets.
   - **Read `docs/decisions/` and respect Status** (D3): `.claude/agents/precedent-reviewer.md:3` (description), `:18-24`, `:45` and `:56`, `.claude/skills/review-branch/references/corpus/recorded-rulings.md:8`, `:59` (cite ADRs by number) and `:74` (a reversal is recorded in the ADR's Rejected section), `.claude/skills/review-branch/SKILL.md:32`, `.claude/agents/plan-reviewer.md:23-28`, `.claude/agents/surface-reviewer.md:66-76`, `.claude/skills/plan-feature/SKILL.md:52` (the planner reads `docs/decisions/README.md` and the ADRs its feature touches) and `:189` (the symbol-removal grep includes `docs/decisions/`).
   - **Role descriptions that move to ADRs** ("records past defects", "considered and declined"): `.claude/agents/surface-reviewer.md:68` and `:148`, `.claude/agents/plan-reviewer.md:200`, `.claude/skills/review-branch/references/finder-base.md:36`, `.claude/skills/plan-feature/SKILL.md:222`, `.claude/skills/plan-feature/references/anti-patterns.md:12` and `:24`, `.claude/skills/fix-bug/SKILL.md:308` (a bug's lesson goes to the issue's DECISIONS.md or an ADR, plus a CLAUDE.md line), `.claude/skills/fix-bug/references/repro-paths.md:11` (ADR 0003).
   - **Surface-reviewer row table** (`.claude/agents/surface-reviewer.md:84-92`), rewritten with ADR numbers, `Accepted` ADRs only: row 1 to 0001 to 0005 and 0011; row 2 to 0006 and 0007; row 3 to `protocol-v1.md` and Netty sources only (D10); row 4 to 0012; row 5 to 0003 and 0009; row 6 to 0008, 0012, 0013, 0016, 0018, 0021, with the enforcement ladder from `references/surface-questions.md`; row 7 to `protocol-v1.md` runtime add/remove, 0016, 0019 (D10).
   - **History citations repointed to ADRs**: `.claude/agents/plan-reviewer.md:54` (D8: open issues in the 0.1 epics), `:75` (§ Module set, forward-compatibility note, to 0016), `:77` (0018), `.claude/skills/plan-feature/SKILL.md:205` (0017 as an out-of-scope source only) and `:136` (heading list), `.claude/skills/plan-feature/references/plan-template.md:43` (0018) and `:155` (Documentation Sync row: CLAUDE.md line plus ADR or DECISIONS.md), `.claude/skills/kendo/references/issue-templates.md:262` (0019) and `:314` (0003), `.claude/skills/kendo/SKILL.md:101` (0023).
   - **Kept, the heading survives**: `§ API design decisions` (`plan-template.md:41-42`, `surface-questions.md:103`, `.claude/skills/task-writer/references/example-client-api-feature.md:47`), `§ Testing / coverage` (`.claude/agents/bug-fix-verifier.md:137`, `plan-reviewer.md:100`, `anti-patterns.md:13`, `surface-questions.md:90`, `issue-templates.md:239`, `security-cost-surface.md:73`, `test-disciplines.md:21`, `finder-base.md:52`), `§ Protocol architecture` (`plan-reviewer.md:76`), `§ Documentation scope` (`plan-reviewer.md:99`).
   - **Build scripts**: `client-api/build.gradle.kts:2` and `client-netty/build.gradle.kts:2` (§ In-flight work (client), to `client-api/CLAUDE.md` / `client-netty/CLAUDE.md`), `server-netty/build.gradle.kts:1` (§ Status, to `server-netty/CLAUDE.md`; drop the em dash).
   - **Generic mentions** ("read CLAUDE.md", "commit CLAUDE.md separately"): `catchup/SKILL.md`, `commit/SKILL.md`, `next/SKILL.md`, `fix-bug/SKILL.md:159,188`, `decisions-template.md`, `issue-templates.md:65,180,199`: checked, unchanged unless they claim CLAUDE.md holds history.
8. **Verification**: run every Acceptance Criteria check; compare the mapping table against the backup; only then commit.

## Security & Cost Surface

### Untrusted wire input and allocation bounds
N/A: documentation only; no code that reads from the wire changes, and ADRs 0001 to 0005 describe existing checks without altering them.
Proof: N/A: no file under `*/src/` is in the diff (`git diff --stat` at PR time).

### Connection lifecycle, timeouts and silent peers
N/A: no connection state, timeout or disconnect behaviour changes; ADRs 0006 and 0007 record existing spec rules.
Proof: N/A: no file under `*/src/` or `protocol/protocol-v1.md` is in the diff.

### Threading and the event loop
N/A: no code runs; nothing touches the event loop. ADR 0017's live enable/disable note is Deferred and binds nothing (D10).
Proof: N/A: no file under `*/src/` is in the diff.

### Resource ownership
N/A: no `ByteBuf`, future or per-connection state is created or released.
Proof: N/A: no file under `*/src/` is in the diff.

### Spec conformance
N/A: `protocol-v1.md` is not edited; ADRs 0001 to 0007 cite its sections for rule text and keep only the reasoning, so there is no second copy to drift.
Proof: N/A: `protocol/protocol-v1.md` is not in the diff; AC 8 checks the ADRs link it.

### Public API surface, module boundaries and enforcement
No public type, method or module dependency changes, so no Javadoc is needed. Existing rules move files but keep their enforcement level: the `ByteBuf` ban is L1 for `client-api` (its `build.gradle.kts` has no Netty dependency, so a Netty import does not compile) and L4 elsewhere; the two-send-paths rule, builder construction and Netty-agnostic `server-netty` signatures stay L4 (review by `plan-reviewer` and `precedent-reviewer`, which step 7 points at the ADRs). The plan adds one structural rule, D3's "only the Decision section of an `Accepted` ADR binds". Its enforcement is L4: `plan-reviewer`, `precedent-reviewer` and `surface-reviewer` are told to read Status and honour it, and `docs/decisions/README.md` states the rule and how a lasting rule gets a CLAUDE.md line. Trade-off: a reviewer or session can still over-read an ADR's Context or Rejected text; the "Does not decide" section and the "binds nothing" opening line of `Deferred`/`Open` ADRs narrow that, and the shape is checked mechanically by AC 3.
Proof: AC 3 (every ADR has all five sections and a Status line) and AC 7 (the three reviewer agents name `docs/decisions/`).

### Runtime endpoint add and remove
N/A: no endpoint registration or routing code changes; ADR 0019 records the existing supported-ids decision.
Proof: N/A: no file under `*/src/` is in the diff.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | Root CLAUDE.md holds only current rules and pointers: no Status, In-flight work, history or epic-sequencing sections; each rule is one line linking its ADR | Read-through, plus check 1 below |
| 2 | Every old heading and bold decision bullet is accounted for | The step 3 mapping table has a row for each (ADR, module or root CLAUDE.md line, "dropped: status" with its Kendo issue or epic, or "dropped: local reference"), checked against the D9 backup before the commit |
| 3 | Every ADR has a Status line and all five sections; `Deferred`/`Open` ADRs say they bind nothing | Check 3 below prints nothing |
| 4 | `docs/decisions/README.md` indexes exactly the ADR files (24), with status meanings and the binding rule | Check 4 below prints two equal numbers; read-through of the rule text |
| 5 | Four module CLAUDE.md files exist, rules only, each linking ADRs | Check 5 below; read-through: no status, "planned" or future-direction lines in the three empty modules |
| 6 | Every CLAUDE.md citation in the repo resolves | Each hit of check 6 below matches a step 7 entry: a kept heading that exists, or an existing `docs/decisions/` file |
| 7 | The reviewer agents read `docs/decisions/` and honour Status | Check 7 below lists all four files twice (once per pattern) |
| 8 | ADRs 0001 to 0007 link `protocol-v1.md` rather than restating it | Check 8 below prints nothing |
| 9 | CLAUDE.md is tracked, `.claude/local-references.md` is ignored, and no committed file or the PR description carries local-only facts | Check 9 below; `/pr` step 2b also runs, and covers what this pattern doesn't (email addresses, `C:\Users\...` paths in added lines) |
| 10 | No em dashes in any new or changed file | Check 10 below prints nothing |

Checks, run in Git Bash from the repo root after `git add` (check 9 reads the index, so the gitignored `local-references.md` is never scanned):

```bash
# 1
grep -nE '^## (Status|In-flight|Frame decoder|Versioning)' CLAUDE.md
# 3
for s in '**Status:**' '## Context' '## Decision' '## Does not decide' '## Rejected'; do
  grep -LF "$s" docs/decisions/0*.md
done
grep -lE '^\*\*Status:\*\* (Deferred|Open)' docs/decisions/0*.md | xargs -r grep -L 'binds nothing'
# 4
ls docs/decisions/0*.md | wc -l; grep -c '^| 00' docs/decisions/README.md
# 5
ls protocol/CLAUDE.md client-api/CLAUDE.md client-netty/CLAUDE.md server-netty/CLAUDE.md
grep -c 'docs/decisions/' protocol/CLAUDE.md client-api/CLAUDE.md client-netty/CLAUDE.md server-netty/CLAUDE.md
# 6
git grep -n 'CLAUDE\.md' -- ':!docs/issues'
# 7
f='.claude/agents/plan-reviewer.md .claude/agents/precedent-reviewer.md .claude/agents/surface-reviewer.md .claude/skills/review-branch/references/corpus/recorded-rulings.md'
grep -l 'docs/decisions' $f; grep -l 'Accepted' $f
# 8
grep -L 'protocol-v1.md' docs/decisions/000[1-7]-*.md
# 9: case-sensitive; the script-development pattern skips the public "claude" repo.
# Bracket expressions keep the pattern from matching (and publishing) the names it hunts for.
git ls-files --error-unmatch CLAUDE.md; git check-ignore .claude/local-references.md
pat='[B]ACKUP|\[\[[a-z-]+\]\]|reference-[k]endo|script-development/[a-bd-z]|\b[A-Za-z]:\\|/User[s]/'
git grep -nE --cached "$pat"
printf '%s' "$PR_BODY" | grep -nE "$pat"   # the PR description, before /pr opens it
# 10
LC_ALL=C.UTF-8 grep -rlP '\x{2014}' CLAUDE.md */CLAUDE.md */build.gradle.kts docs/ .claude/
```

## Reuse and patterns

- The old `CLAUDE.md` (untracked, backed up per D9): call: the source text for every ADR; decisions are moved, not reworded into new rulings.
- `.claude/skills/plan-feature/references/decisions-template.md`: mirror: the Context / Decision / Rejected shape for ADRs.
- `protocol/protocol-v1.md`: call: ADRs 0001 to 0007 link its sections instead of restating wire rules.
- `.gitignore`: mirror: the existing `### Claude Code ###` block holding `.claude/settings.local.json`.

## Testing Strategy

N/A: no code changes, so no test classes. Verification is the checks in Acceptance Criteria;
`./gradlew build` still runs at `/pr` and must stay green.

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: no wire rule changes |
| New or changed public type or method in `client-api` (or `protocol`, where public) | Javadoc on that type | N/A: no code changes |
| A design decision, layer rule, convention or module plan this feature makes, changes, deletes or renames | `CLAUDE.md` | Update: this issue is the rewrite; also `.claude/skills/plan-feature/references/plan-template.md:155`, whose Documentation Sync row names CLAUDE.md sections that no longer exist |

## Edge Cases
- **Sections mixing a decision with status** (Status, In-flight work (client), Workflow going forward): each decision becomes an ADR (0012, 0016, 0018, 0019, 0022, 0023); the status becomes a "dropped: status" row naming the Kendo issue or epic that holds it.
- **Bug-fix narratives** (VarInt width, dead checks, array allocation): become the Context of the ADR whose rule they produced, never a separate rule.
- **Future-facing notes** (module system, live enable/disable, hot-swap, docs site, style tool): `Deferred` or `Open` ADRs that open with "binds nothing in 0.1"; reviewers no longer draw next consumers from them (D8, D10).
- **Citations describing CLAUDE.md's role** ("records what was declined", "real bugs recorded in CLAUDE.md"): reworded to name `docs/decisions/`, not just repointed.
- **A cited heading that survives while its content moves** (`§ Module set (in-flight packets during disable)`): repointed to the ADR, not kept on the strength of the heading.
- **A claim in the old file that no longer matches the code**: the ADR records the code as it is. Found so far: `ProtocolException`'s debug accessors are Lombok-generated from the static `debugStackTraces` field, not written out.

## Risks
- **A decision lost in the split.** Watch: the step 3 mapping table built from the old file's headings and bold bullets, checked against the D9 backup before the commit.
- **Over-steering**: an ADR's Context or Rejected text read as a requirement. Watch: D3's rule in the index and CLAUDE.md, "Does not decide" on every ADR, and D8 keeping Deferred ADRs out of design reviews.
- **Reviewer agents lose the declined alternatives and defect history they rely on.** Watch: step 7 and AC 7.
- **Private content published.** Watch: D7 ordering, D9's backup kept outside the repo, the gitignored `local-references.md`, check 9 over the index and the PR description (which `/pr` step 2b doesn't scan), and the `/pr` privacy check.
- **ADRs restating the spec and drifting from it.** Watch: AC 8.
