# 0024: Docs site

**Status:** Deferred

## Context

Documentation beyond the repository was discussed in early planning. No work has started.

## Decision

This ADR binds nothing in 0.1. Recorded direction, for when this work is planned:

- A docs site at bendprotocol.net, VitePress-style.
- Versioned Javadoc published separately, accumulating one tree per version (e.g. S3 plus
  CloudFront), because static-site hosts like Cloudflare Pages typically replace each deploy
  rather than accumulate.

## Does not decide

- Hosting, tooling or domain. The above is direction, not a choice.
- What the site contains. Javadoc scope is [ADR 0021](/docs/decisions/0021-javadoc-scope.md).

## Rejected

None recorded.
