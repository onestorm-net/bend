# 0019: Supported endpoint ids are sent as a full snapshot

**Status:** Accepted

## Context

A client tells the server which endpoint ids it can handle
([protocol-v1.md § Supported Endpoints](/protocol/protocol-v1.md)): the intersection of the
ids the server has advertised as available and the ids the client has at least one local
registration for, incoming or outgoing. Outgoing registrations count because the server blocks
any client `Endpoint Message` on an id outside the supported list
([protocol-v1.md § Endpoint message routing](/protocol/protocol-v1.md)). Corrected in BD-0003
([DECISIONS.md](/docs/issues/BD-0003-endpoint-registration-typed-handles/DECISIONS.md) D4); the
first version counted only ids with local handlers and had no unregister trigger.

## Decision

The client resends the whole supported set, not a delta, on each of three triggers:

- A new `Available Endpoints` list arrives, including one that drops a registered id.
- An endpoint is registered locally, in either direction, for an id in the available list.
- An endpoint is unregistered locally, leaving an available id with no registration.

A snapshot is simpler, idempotent, and self-healing after a reconnect.

## Does not decide

- Revisit only if resending the whole set becomes a measured efficiency problem.
- Whether a registration whose direction the server's endpoint flags don't permit (an outgoing
  registration for a clientbound-only endpoint) counts toward the set. The client-netty epic
  decides.

## Rejected

- Deltas: more state to keep consistent, for no measured gain.
