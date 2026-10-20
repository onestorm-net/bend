# 0020: The first release is 0.x

**Status:** Accepted

## Context

`client-api` (and later `server-api`) will change shape while real use teaches what they need.
The first public release realistically has one user: the author.

## Decision

- The first public release is versioned `0.x`, not `1.0`, leaving room to break `client-api`
  before committing to semver guarantees. Public does not have to mean stable.
- All modules share one version, set once at the root (`allprojects { version = ... }`),
  currently `0.1.0-SNAPSHOT`. The `-SNAPSHOT` suffix is dropped when `0.1.0` is tagged.

## Does not decide

- What `1.0` requires.

## Rejected

- `1.0.0` (the version before the Gradle migration): promises stability the API doesn't have.
