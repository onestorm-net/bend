# 0021: Javadoc covers public contract modules only

**Status:** Accepted

## Context

Publishing Javadoc implies a stability contract. Implementation modules change internally
without notice, and documenting them publicly would promise otherwise.

## Decision

- Javadoc covers the public contract modules: `client-api`, `server-api` once it exists, and
  `protocol` where it is treated as a shared public spec and types module.
- Implementation modules (`client-netty`, `server-netty`) and internal utilities get no Javadoc.
- The test per module: "If I refactor this class's internals next month, do I owe anyone a
  heads-up?" Yes: public API, Javadoc it. No: keep it out.

## Does not decide

- How Javadoc is published or hosted; see [ADR 0024](/docs/decisions/0024-docs-site.md).
- Which parts of `protocol` count as public.

## Rejected

None recorded.
