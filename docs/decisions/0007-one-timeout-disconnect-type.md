# 0007: One `Timeout` disconnect type

**Status:** Accepted
**Date:** 2026-09-17

## Context

`Disconnect` carries a numeric `Type` a client can branch on (retry, report a version mismatch,
give up) and a free-text `Reason` for logging, which isn't safe to match on. Mapping the seven
server-side disconnect conditions onto the four existing types showed that bad packet order,
invalid built-in packets and both endpoint-message violations collapse into `1: Protocol error`
without losing an actionable distinction. Two conditions (no `Handshake` within 30 seconds, no
`Available Endpoints Acknowledged` within 30 seconds) are timeouts, but the only timeout type was
`3: Timeout (Keep-alive)`, which mislabels them.

## Decision

Type `3` is renamed to plain `Timeout` and covers every timeout. The specific cause goes in
`Reason`. See [protocol-v1.md § Disconnect](../../protocol/protocol-v1.md) for the type table.

## Does not decide

- Adding a type later if a client needs to react differently to different timeouts.

## Rejected

- A new type per timeout: all three call for the same client reaction (reconnect or retry), so
  separate types add nothing a client would branch on.
