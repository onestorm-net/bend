# 0016: The 0.1 module set has no `server-api`

**Status:** Accepted

## Context

Runtime add and remove of endpoints is a core protocol feature
([protocol-v1.md § Endpoint list updates](/protocol/protocol-v1.md)), not an add-on.
`client-api` has external consumers from day one: anyone writing a client. An API module only
earns its keep once something other than its one implementation depends on it, and in 0.1
nothing can load independent code into a running server, so a `server-api` would have no user.

## Decision

- The 0.1 modules are `protocol`, `client-api`, `client-netty` and `server-netty`. There is no
  `server-api`.
- Endpoint register, unregister and routing live directly in `server-netty`.
- The dividing line: if a caller can call `server-netty`'s registration methods (e.g.
  `endpoints().register(id, handler)` / `.unregister(id)`) and get correct protocol behaviour
  (advertise, withdraw, route), that is 0.1. Bend tracking *ownership* of registrations and
  managing their lifecycle on someone else's behalf is the module system
  ([ADR 0017](/docs/decisions/0017-module-system.md)), not 0.1.
- In 0.1, code that registers endpoints calls `server-netty` directly, compiled into the same
  binary. Don't half-build a loading mechanism to make that feel more complete.
- `server-netty`'s register, unregister and endpoint-handler signatures stay Netty-agnostic, so
  they can be lifted into an API module later as a mechanical move rather than an API-carving
  exercise.

## Does not decide

- When `server-api` is extracted. The recorded intent is 0.2, as folder `server-api` and Gradle
  project `:bend-server-api` ([ADR 0014](/docs/decisions/0014-gradle-build-layout.md)), together with module
  loading.
- The shape of those signatures beyond keeping Netty types out of them.

## Rejected

- Extracting `server-api` in 0.1: an abstraction with no user.
- The pre-Gradle empty `bend-server-api` module: created ahead of schedule, deleted in the
  migration.
