# Decisions

Architecture decision records (ADRs) for Bend: why things are built the way they are, and what
was considered and rejected. The rules themselves are in `CLAUDE.md` (one line each, linking
here) and in `protocol/protocol-v1.md` for anything on the wire.

## What binds

Each ADR has exactly one status:

| Status | Meaning |
|---|---|
| `Accepted` | A rule in force. Its **Decision** section binds. |
| `Deferred` | Direction for later work. Binds nothing now. |
| `Open` | An unresolved question, with what is known so far. Binds nothing. |

Only the **Decision** section of an `Accepted` ADR is a rule. Everything else in an ADR is
background:

- **Context** explains why the question came up, often with the bug or history behind it.
- **Does not decide** names what the ADR leaves open. Treat it as open; don't fill the gap
  with the ADR's spirit.
- **Rejected** records alternatives and why they lost. Don't re-propose one without a new
  reason, but don't treat a rejection as a rule either.

Never design code toward a `Deferred` or `Open` ADR. If a feature seems to need one, raise it
as a decision for that feature instead.

## Adding a decision

New decisions are recorded in the issue's own `docs/issues/<branch>/DECISIONS.md` while it is
planned. A decision that becomes a lasting rule also gets a one-line entry in `CLAUDE.md` (or
the module's `CLAUDE.md`) linking to where its reasoning lives. Changing an ADR's Decision is a
decision in its own right: record it in the issue that changes it, and update the ADR.

## Shape

```markdown
# NNNN: <title>

**Status:** Accepted | Deferred | Open
**Date:** <only where known>

## Context
## Decision
## Does not decide
## Rejected
```

A `Deferred` or `Open` ADR opens its Decision section with "This ADR binds nothing in 0.1."

## Index

| ADR | Title | Status |
|---|---|---|
| 0001 | [Frame length prefix is a 28-bit VarInt with a separate configurable cap](/docs/decisions/0001-frame-length-28-bit-varint.md) | Accepted |
| 0002 | [NUL padding before a frame is skipped](/docs/decisions/0002-nul-padding-skip.md) | Accepted |
| 0003 | [Zero-length frames are rejected; canonical VarInts are not required](/docs/decisions/0003-zero-length-frames-rejected.md) | Accepted |
| 0004 | [Array lengths are validated before allocating](/docs/decisions/0004-array-length-validated-before-allocation.md) | Accepted |
| 0005 | [Endpoint flags and ids are validated](/docs/decisions/0005-endpoint-validation.md) | Accepted |
| 0006 | [Client-side disconnects, wrong-direction messages, keep-alive echo](/docs/decisions/0006-client-disconnects-and-keep-alive-echo.md) | Accepted |
| 0007 | [One `Timeout` disconnect type](/docs/decisions/0007-one-timeout-disconnect-type.md) | Accepted |
| 0008 | [Two protocol layers, two send paths](/docs/decisions/0008-two-protocol-layers.md) | Accepted |
| 0009 | [Unreachable defensive checks are deleted](/docs/decisions/0009-unreachable-checks-deleted.md) | Accepted |
| 0010 | [Coverage reported as XML and CSV, no gate yet](/docs/decisions/0010-coverage-reporting.md) | Accepted |
| 0011 | [`ProtocolException` for wire input](/docs/decisions/0011-protocol-exception.md) | Accepted |
| 0012 | [No Netty types in public API modules](/docs/decisions/0012-no-netty-types-in-api.md) | Accepted |
| 0013 | [Module naming](/docs/decisions/0013-module-naming.md) | Accepted |
| 0014 | [Gradle build layout](/docs/decisions/0014-gradle-build-layout.md) | Accepted |
| 0015 | [Style enforcement tooling](/docs/decisions/0015-style-enforcement-tooling.md) | Open |
| 0016 | [The 0.1 module set has no `server-api`](/docs/decisions/0016-module-set-0-1.md) | Accepted |
| 0017 | [Module system](/docs/decisions/0017-module-system.md) | Deferred |
| 0018 | [A `Client` is built by its implementation's builder](/docs/decisions/0018-client-built-by-implementation-builder.md) | Accepted |
| 0019 | [Supported endpoint ids are sent as a full snapshot](/docs/decisions/0019-supported-ids-full-snapshot.md) | Accepted |
| 0020 | [The first release is 0.x](/docs/decisions/0020-first-release-0-x.md) | Accepted |
| 0021 | [Javadoc covers public contract modules only](/docs/decisions/0021-javadoc-scope.md) | Accepted |
| 0022 | [Workflow: branches and commit messages](/docs/decisions/0022-workflow-branches-and-commits.md) | Accepted |
| 0023 | [Epic structure for 0.1](/docs/decisions/0023-epic-structure-0-1.md) | Accepted |
| 0024 | [Docs site](/docs/decisions/0024-docs-site.md) | Deferred |
