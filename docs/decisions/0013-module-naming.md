# 0013: Module naming

**Status:** Accepted

## Context

A generic `-impl` suffix stops working the moment an API has a second implementation (a Netty
client and a later non-Netty client), and forces a rename.

## Decision

- The public, stable contract gets the plain name: `client-api`, not `client` plus
  `client-interfaces`.
- An implementation gets a suffix naming its transport or mechanism, never `-impl`. The
  implementation modules are `client-netty` and `server-netty`.

## Does not decide

- Whether a second implementation will exist. The rule only keeps the name stable if one does.

## Rejected

- `-impl`: breaks as soon as there are two implementations.
