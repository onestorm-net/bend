# BD-0008-split-claude-md: Decisions

## D1: One ADR per decision
**Status:** Accepted

**Context:** CLAUDE.md's history has to move into `docs/decisions/`; the split could follow today's sections or the individual rulings.

**Decision:** One ADR per decision (24). A bug-fix story becomes the Context of the ADR it produced, not its own ADR.

**Rejected:**
- One doc per topic (8): one file would hold several rulings, so superseding one means editing a doc that still holds valid ones.

## D2: A reversed decision is one ADR with its history inside
**Status:** Accepted

**Context:** The client-construction ruling was decided (ServiceLoader, 2026-09-17) and reversed (implementation builder, 2026-10-04).

**Decision:** One ADR for the rule in force; the reversed option sits under Rejected with both dates and why it lost. No ADR describes a rule that isn't in force.

**Rejected:**
- Two ADRs linked by `Superseded by`: a reader can land on the dead ruling first.

## D3: Only an ADR's Decision section binds
**Status:** Accepted

**Context:** The developer has seen a model treat an issue's note about scalability and the future as a requirement. Recorded reasoning can over-steer the same way.

**Decision:** Every ADR has a Status (`Accepted` binds, `Deferred` and `Open` bind nothing) and a "Does not decide" section. The index states that Context, Rejected and future notes are background, and no code is designed toward a `Deferred` ADR.

**Rejected:**
- Relying on careful wording alone: nothing tells a later reader which sentences are rules.

## D4: Four module CLAUDE.md files, rules only
**Status:** Accepted

**Context:** Only `protocol` has code; the other three modules are empty skeletons but already have module-specific rules.

**Decision:** `protocol`, `client-api`, `client-netty` and `server-netty` each get a CLAUDE.md. The three empty modules carry only rules already decided, nothing that sets a direction for code that doesn't exist yet.

**Rejected:**
- `protocol` only: the empty modules' rules would stay in the root file, which this issue shrinks.

## D5: Local-only facts go in a gitignored `.claude/local-references.md`
**Status:** Accepted

**Context:** Some facts are true only on one machine (a pre-migration client copy, local clones of upstream projects) and must not reach the public repo.

**Decision:** Each developer may create `.claude/local-references.md` (gitignored, next to `settings.local.json`). CLAUDE.md explains its format and that Claude reads it before fetching upstream source from the web, but committed files cite public URLs only: no local paths, local folder names or memory pointers.

**Rejected:**
- Repo root: it is Claude-only setup, and `.claude/` already holds the one other per-developer file.
- `CLAUDE.local.md`: loaded every session, so every path costs context, and personal rules would accrete there.

## D6: Keep the section names the skills cite
**Status:** Accepted

**Context:** Citations across `.claude/` name CLAUDE.md sections (`§ API design decisions`, `§ Testing / coverage`, `§ Module set`, ...).

**Decision:** The new CLAUDE.md groups its one-line rules under those same headings where the rules still exist, so most citations stay valid. Citations that point at history are repointed to the ADR.

**Rejected:**
- Repointing every citation to an ADR: more churn, and a citation then lands on reasoning rather than on the rule.

## D7: The old CLAUDE.md is never committed
**Status:** Accepted

**Context:** Un-ignoring CLAUDE.md and committing it before the rewrite would publish its status lines and local-only facts in git history for good.

**Decision:** The `.gitignore` change and the first tracked CLAUDE.md land in the same commit, with the rewritten file only.

**Rejected:**
- Committing the old file first to diff against: the history it would publish is exactly what this issue keeps out.

## D8: plan-reviewer takes "next consumers" from the 0.1 backlog
**Status:** Accepted

**Context:** `plan-reviewer.md:54` takes a plausible next consumer from CLAUDE.md's "Deferred to 0.2+" list for its shallow-module test. Under D3 that list becomes Deferred ADR 0017, which binds nothing.

**Decision:** The reviewer takes next consumers from open issues in the 0.1 epics instead, so a Deferred ADR never feeds a design review.

**Rejected:**
- Allowing ADR 0017 for the test only: it still pulls 0.2+ ideas into 0.1 design discussions, the over-steering D3 exists to stop.

## D9: Back up the old CLAUDE.md outside the repo before overwriting
**Status:** Accepted

**Context:** The old CLAUDE.md is untracked and the repo has no history of it, so a decision missed after the rewrite would be unrecoverable.

**Decision:** Copy it outside the working tree before step 6 overwrites it; record the copy's location in `.claude/local-references.md`, not in any committed file.

**Rejected:**
- A gitignored copy inside the repo: one mistyped ignore rule publishes it.
- The session scratchpad: temporary, and this work may span sessions.

## D10: "Enable/disable while live" stays in Deferred ADR 0017
**Status:** Accepted

**Context:** The old file resolved that modules can be enabled and disabled on live connections, and `surface-reviewer.md` rows 3 and 7 cite it as a live standard.

**Decision:** It is a requirement on the 0.2+ module system, so it stays in ADR 0017 and binds nothing in 0.1. Rows 3 and 7 cite `protocol-v1.md`'s runtime endpoint add/remove and ADR 0016's dividing line, which hold for 0.1.

**Rejected:**
- An Accepted ADR of its own: it would bind 0.1 code to a module system that doesn't exist.

## D11: The docs site is its own Deferred ADR
**Status:** Accepted

**Context:** The old Documentation scope section holds a ruling (Javadoc for public contract modules only) and a plan (docs site, versioned Javadoc hosting).

**Decision:** ADR 0021 (Javadoc scope, Accepted) and ADR 0024 (docs site, Deferred), so every ADR has exactly one of the three statuses (D3).

**Rejected:**
- One ADR with a mixed status: a fourth status value that D3, the index and AC 3 can't see.

## D12: Repo file links are written from the root, as `/path`
**Status:** Accepted

**Context:** The split left the same target written three ways (`docs/...`, `../docs/...`, `../../protocol/...`) depending on where the linking file sits, and relative links break when that file moves.

**Decision:** Every Markdown link to a repo file starts with `/` and is written from the repository root, e.g. `[0008](/docs/decisions/0008-two-protocol-layers.md)`. GitHub documents that "Links starting with `/` will be relative to the repository root"; templates use placeholders such as `/docs/issues/<branch-name>/PLAN.md`.

**Rejected:**
- Relative links (`../../`): differ per file and break on a move.
- Plain root paths as code text, no link: unambiguous, but not clickable on GitHub.

## D13: ADR 0012 bans every Netty type, not only `ByteBuf`
**Status:** Accepted

**Context:** Branch review flagged that ADR 0012 names `Channel`, `ChannelFuture` and `EventLoop` where the old rule named only `ByteBuf`, and that its Decision carried the one-off `EndpointCodec` rework.

**Decision:** The developer confirmed the rule covers all Netty types: the old text named `ByteBuf` only because it was the one type that had leaked into the API, and the dependency and versioning reasons apply to any Netty type. The `EndpointCodec` rework moves to Context as an example; it is tracked work, not a rule.

**Rejected:**
- Narrowing the ban to `ByteBuf`: leaves the other Netty types allowed for no reason.
