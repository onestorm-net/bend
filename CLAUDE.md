# Bend

Bend is a Netty-based client/server framework on its own wire protocol, in which endpoints can
be added and removed while connections are live.

This file holds the rules that are true now, one line each. The reasoning behind each rule, and
the alternatives that were rejected, is in the linked ADR under `docs/decisions/`. Status and
planned work live in the tracker, not here.

## Modules

| Folder | Gradle project | What it is |
|---|---|---|
| `protocol/` | `:bend-protocol` | Wire types, packets, frame codec; spec in `protocol/protocol-v1.md` |
| `client-api/` | `:bend-client-api` | Public contract for code that uses a client |
| `client-netty/` | `:bend-client-netty` | Netty implementation of `client-api` |
| `server-netty/` | `:bend-server-netty` | Netty server, including endpoint registration |

Each module has its own `CLAUDE.md` with the rules specific to it.

## Where things live

- Wire spec: `protocol/protocol-v1.md`. It is the source of truth for anything on the wire.
- Decisions and their reasoning: `docs/decisions/` (start at its `README.md`).
- Per-issue plans and decisions: `docs/issues/<branch>/` (`PLAN.md`, `DECISIONS.md`,
  `TASKS.md`, or `BUG.md`).
- Issues, epics and status: Kendo, project Bend (`BD-NNNN` keys), through the `/kendo` skill.
- Code: the public GitHub repository `onestorm-net/bend`.

## How recorded decisions bind

- Only the **Decision** section of an `Accepted` ADR is a rule. Context, Rejected and Does not
  decide are background; a `Deferred` or `Open` ADR binds nothing.
- Never design code toward a `Deferred` ADR. If a feature seems to need one, raise it as a
  decision for that feature.
- A rule in this file links its ADR; when they disagree, the ADR's Decision section wins and
  this file gets fixed.
- New decisions go in the issue's `DECISIONS.md`; a lasting rule also gets a line here.

## Protocol architecture

- Built-in packets go to the channel through an internal path; user payloads go through the
  public, registration-checked send path wrapped in `Endpoint Message`. Never mix the two.
  ([0008](/docs/decisions/0008-two-protocol-layers.md))
- Frame and input rules (length width, padding, zero-length frames, array lengths, endpoint
  validation) are in `protocol/CLAUDE.md` and ADRs 0001 to 0006.

## API design decisions

- No Netty types (`ByteBuf`, `Channel`, `ChannelFuture`, `EventLoop`) in a public API module;
  payloads cross as `java.nio.ByteBuffer`, converted inside the `-netty` modules.
  ([0012](/docs/decisions/0012-no-netty-types-in-api.md))
- Malformed or oversized wire input raises `ProtocolException`; caller misuse at setup time
  raises `IllegalArgumentException`. ([0011](/docs/decisions/0011-protocol-exception.md))
- A `Client` comes from its implementation's builder, which returns the `client-api` `Client`
  interface; `client-api` depends on nothing.
  ([0018](/docs/decisions/0018-client-built-by-implementation-builder.md))
- Lombok is for `protocol` and the `-netty` modules only; an API module never uses it, not even
  `compileOnly`. (BD-0001 D8,
  [DECISIONS.md](/docs/issues/BD-0001-namespaced-id-value-type/DECISIONS.md))

## Testing / coverage

- Tests use JUnit Jupiter and AssertJ; a test class is named `<ClassUnderTest>Test` in the
  mirrored package under `src/test/java`, with methods named `method_condition_expected`. This
  is codebase convention (see the existing tests), not an ADR ruling.
- Coverage reports are XML and CSV under `<module>/build/reports/jacoco/test/`; there is no
  coverage gate. ([0010](/docs/decisions/0010-coverage-reporting.md))
- A defensive check for a case the current code can't produce is deleted, not kept; a missed
  line is a question of reachability, never a reason for a contrived test.
  ([0009](/docs/decisions/0009-unreachable-checks-deleted.md))

## Module naming convention

- The API module gets the plain name (`client-api`); an implementation gets a suffix saying
  what it is, i.e. what it's built on (`client-netty`), never the generic `-impl`, which only
  works while there is a single implementation. ([0013](/docs/decisions/0013-module-naming.md))

## Module set

- 0.1 is `protocol`, `client-api`, `client-netty`, `server-netty`; no `server-api`.
  Registration lives in `server-netty` with Netty-agnostic signatures; don't half-build a module
  loading mechanism. ([0016](/docs/decisions/0016-module-set-0-1.md))
- The module system is Deferred and binds nothing in 0.1.
  ([0017](/docs/decisions/0017-module-system.md))

## Build tooling

- Always `./gradlew` (wrapper 9.7.1, JDK 25 toolchain), never a bare `gradle`. Use the Gradle
  project path in commands: `./gradlew :bend-protocol:test --rerun`.
  ([0014](/docs/decisions/0014-gradle-build-layout.md))
- Dependency versions live in `gradle/libs.versions.toml`.
  ([0014](/docs/decisions/0014-gradle-build-layout.md))
- There is no linter or formatter yet; which tool to add is Open.
  ([0015](/docs/decisions/0015-style-enforcement-tooling.md))
- Versions are `0.x`, one version for all modules, set at the root.
  ([0020](/docs/decisions/0020-first-release-0-x.md))

## Documentation scope

- Javadoc covers the API modules (`client-api`, later `server-api`), not implementation
  modules. Whether `protocol` gets Javadoc is not decided.
  ([0021](/docs/decisions/0021-javadoc-scope.md))

## Workflow

- `development` is the integration branch; `main` only moves through a release PR. Issue
  branches are `BD-NNNN-<slug>`, small issue-less changes `<type>/<slug>`, all PR'd into
  `development`. ([0022](/docs/decisions/0022-workflow-branches-and-commits.md))
- Commit messages and PR titles end with the issue key: `(BD-NNNN)`; on a keyless
  `<type>/<slug>` branch they have no suffix.
  ([0022](/docs/decisions/0022-workflow-branches-and-commits.md))
- The skills in `.claude/skills/` carry the step-by-step workflow (`/newbranch`,
  `/plan-feature`, `/task-writer`, `/next`, `/commit`, `/review-branch`, `/pr`); see
  `.claude/README.md`.

## Local references

Each developer may keep a gitignored `.claude/local-references.md` listing machine-specific
locations: local clones of upstream projects (Netty, Velocity), older copies of this code, and
similar. Create it yourself; it is never committed.

- Format: a table of name, absolute path, and what the entry is for.
- When you need upstream or related source, read `.claude/local-references.md` first and use a
  listed local copy before fetching from the web.
- Never copy a local path, local folder name or machine detail into a committed file. Cite the
  public URL (for example the project's GitHub page) instead.

## Writing style

- No em dashes in prose or docs; use commas, periods or parentheses.
- Link repo files from the root with a leading `/`, e.g.
  `[spec](/protocol/protocol-v1.md)`, never `../`. (BD-0008 D12,
  [DECISIONS.md](/docs/issues/BD-0008-split-claude-md/DECISIONS.md))
