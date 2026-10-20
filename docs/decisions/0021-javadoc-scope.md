# 0021: Javadoc covers public contract modules only

**Status:** Accepted

## Context

Publishing Javadoc implies a stability contract. Implementation modules change internally
without notice, and documenting them publicly would promise otherwise.

## Decision

- Javadoc covers the two API modules: `client-api`, and `server-api` once it exists.
- Implementation modules (`client-netty`, `server-netty`) and internal utilities get no Javadoc.
- The test per module: "If I refactor this class's internals next month, do I owe anyone a
  heads-up?" Yes: public API, Javadoc it. No: keep it out.

## Does not decide

- How Javadoc is published or hosted; see [ADR 0024](/docs/decisions/0024-docs-site.md).
- Whether `protocol`, or part of it, gets Javadoc. The earliest note listed it only "if treated
  as a shared public spec and types module", and that has not been settled. One input for when
  it is: `client-api` depends on nothing ([ADR 0018](/docs/decisions/0018-client-built-by-implementation-builder.md)),
  so code that uses a client never sees `protocol` types; the wire contract for peer
  implementers is `protocol/protocol-v1.md`.

## Rejected

None recorded.
