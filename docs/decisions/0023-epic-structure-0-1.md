# 0023: Epic structure for 0.1

**Status:** Accepted
**Date:** 2026-09-17

## Context

An epic groups issues on a timeline. The protocol's design and implementation were already
done when the tracker was set up, with no issues to attach and none planned. Its full record is
in [protocol-v1.md](/protocol/protocol-v1.md) and ADRs 0001 to 0007.

## Decision

- The 0.1 epics are `client-api`, `client-netty`, `server-netty` and `Release hygiene` (Gradle
  finalized, README and usage example, client to server round-trip tests, tag `0.1.0`).
- They run in that order. `Release hygiene` closes the sequence because its remaining work
  needs a working client and server to test against.
- There is no `Protocol` epic.
- `server-api` gets no epic until `Release hygiene` closes and `0.1.0` is tagged; it is not part
  of the 0.1 sequence ([ADR 0016](/docs/decisions/0016-module-set-0-1.md)).

## Does not decide

- Epics outside the 0.1 sequence, such as tooling work.

## Rejected

- A `Protocol` epic: it would sit permanently empty. One was briefly created, then deleted for
  this reason.
