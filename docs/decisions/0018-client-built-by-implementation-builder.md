# 0018: A `Client` is built by its implementation's builder

**Status:** Accepted
**Date:** 2026-10-04

## Context

How a caller gets a `Client` decides what `client-api` has to hold. Every client setting is
implementation-specific, and a `Client` should be safe to hand to code that must not
reconfigure or reconnect it.

## Decision

- A `Client` is created by the implementation's own builder, e.g.
  `NettyClient.builder()...connect()`.
- `client-api` holds no configuration: the `Client` interface has no `connect()` and no
  settings. A `Client` can be shared without anyone changing its settings or connecting it twice.
- `client-api` depends on nothing: not on `protocol` (Netty-shaped throughout), not on Netty. It
  holds interfaces and its own small value types (namespaced id, direction).
- The builder's `connect()` returns the `Client` interface, and the concrete client class is
  package-private, so callers can't name or cast to it. The builder is the only `client-netty`
  type a caller touches. No JPMS is needed for this.

Trade-off accepted: code that constructs a client imports `client-netty`. Code that only uses a
client depends on `client-api` alone, which is where transport independence matters
([ADR 0013](/docs/decisions/0013-module-naming.md)).

## Does not decide

- The builder's settings or method names.
- Whether downstream code wrapping Bend exposes only the `Client` interface; that is its choice.

## Rejected

- A `ServiceLoader`-discovered factory on `client-api` (decided 2026-09-17, reversed
  2026-10-04): it only helps when the API defines a common configuration surface (the
  `java.net.http.HttpClient.newBuilder()` shape). With every setting implementation-specific, a
  discovered factory could only hand out a default-configured client.
