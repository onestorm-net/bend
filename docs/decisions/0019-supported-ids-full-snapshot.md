# 0019: Supported endpoint ids are sent as a full snapshot

**Status:** Accepted

## Context

A client tells the server which endpoint ids it can handle
([protocol-v1.md § Supported Endpoints](/protocol/protocol-v1.md)): the intersection of the
ids the server has advertised as available and the ids the client has local handlers for. That
set changes when a new available-endpoints list arrives, or when a handler is registered for an
id that is in the available list.

## Decision

On each of those two triggers, the client resends the whole supported set, not a delta. A
snapshot is simpler, idempotent, and self-healing after a reconnect.

## Does not decide

- Revisit only if resending the whole set becomes a measured efficiency problem.

## Rejected

- Deltas: more state to keep consistent, for no measured gain.
