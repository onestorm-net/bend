# 0013: Module naming

**Status:** Accepted

## Context

A generic `-impl` suffix stops working the moment an API has a second implementation (a Netty
client and a later non-Netty client), and forces a rename.

## Decision

- The public, stable contract gets the plain name: `client-api`, not `client` plus
  `client-interfaces`.
- An implementation gets a suffix that says what the implementation is: what it is built on
  or how it works. Bend's are built on Netty, so the implementation modules are `client-netty`
  and `server-netty`. Never the generic `-impl`.

## Does not decide

- Whether a second implementation will exist. The rule only keeps the name stable if one does.

## Rejected

- `-impl`: says nothing about the implementation, so it only works while there is exactly one,
  and breaks (forcing a rename) as soon as there are two.
