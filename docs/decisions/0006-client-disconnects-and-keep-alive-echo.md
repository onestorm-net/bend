# 0006: Client-side disconnects, wrong-direction messages, keep-alive echo

**Status:** Accepted
**Date:** 2026-09-16

## Context

A spec-versus-implementation audit found three gaps in `protocol-v1.md`:

- The spec only said when the server must disconnect the client. A client written from the
  spec had no guidance when the server never answers, goes silent or misbehaves.
- Only "unsupported endpoint" was a disconnect condition for `Endpoint Message`. A message on a
  supported, available endpoint but in a direction its flags don't allow fell through.
- Keep Alive constrained timing only. A client could echo a constant value forever and stay
  compliant without proving it read anything.

## Decision

- [protocol-v1.md § Client-side disconnect conditions](../../protocol/protocol-v1.md) mirrors
  the server-side section. Its timeouts (handshake response, keep-alive received) reuse the
  spec's existing 30-second constant rather than a new number.
- An `Endpoint Message` sent in a direction its endpoint's flags don't permit is a disconnect
  condition on both sides, at the same tier as an unsupported endpoint.
- The server MUST verify that the echoed Keep Alive value matches what it sent, and treats a
  mismatch as a missed response.

## Does not decide

- How an implementation schedules or cancels these timeouts.

## Rejected

- A new timeout constant for the client-side waits: the spec's 30 seconds already fits.
