# 0005: Endpoint flags and ids are validated

**Status:** Accepted
**Date:** 2026-09-16

## Context

[protocol-v1.md § Types](../../protocol/protocol-v1.md) (Namespaced Id/Key, Endpoint) had MUST
rules that nothing checked: flags must set at least one direction bit, and an id must be
`namespace:key` using only lowercase `a-z0-9_-` in each half, at most 64 characters. Only the
UTF-8 byte-length cap was enforced, through `ProtocolUtil.readString`.

## Decision

The `Endpoint` constructor enforces both rules, so they hold for `Endpoint.of` and for wire
decode alike:

- `flags == 0` raises `ProtocolException`.
- The id is checked against a `namespace:key` regex and the 64-character limit, raising
  `ProtocolException`.

## Does not decide

- How `client-api` represents ids. Its own value types are separate from `protocol`
  ([ADR 0018](0018-client-built-by-implementation-builder.md)).

## Rejected

None recorded.
