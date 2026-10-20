# 0017: Module system

**Status:** Deferred

## Context

Beyond 0.1, the idea is that independently written modules can be loaded into a running
server. This ADR records what has been said about that so it isn't lost, without making any of
it a requirement now.

## Decision

This ADR binds nothing in 0.1. Nothing in 0.1 is designed toward it; see
[ADR 0016](/docs/decisions/0016-module-set-0-1.md) for what 0.1 does instead.

Recorded direction, for when this work is planned:

- A Bukkit-style `Module` interface with `onEnable` / `onDisable`, and dependencies between
  modules (`depends` / `softDepends`) resolved in topological order (e.g. Kahn's algorithm).
  Enable in dependency order; disable in reverse, so nothing active relies on something already
  torn down.
- Auto-teardown: Bend, not the module author, tracks which endpoints a module registered, so
  disabling a module unregisters everything it owns.
- Loading modules from jars, with hot-swapping a module's jar on a running server as the actual
  goal.
- Modules can be enabled and disabled while connections are live, which follows from
  hot-swapping. `disable()` then has to handle an `Endpoint Message` already dispatched to a
  handler whose module is being torn down, not only stop future messages.

## Does not decide

- Anything about 0.1 code, including how `server-netty` registration works today.
- The module API, the loading mechanism, classloading, or the jar format.
- Whether the directions above survive when this work is actually planned.

## Rejected

None recorded.
