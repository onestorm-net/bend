# BD-0001-namespaced-id-value-type: Decisions

## D1: `NamespacedId` is a final class with `get` accessors
**Status:** Accepted

**Context:** The value type could be a record or a hand-written final class.

**Decision:** A final class with a private constructor, `getNamespace()`, `getKey()` and `asString()`, so the public API reads like BD-0003's `Endpoint#getId()` and `protocol`'s Lombok `getX` accessors (`Endpoint.java`).

**Rejected:**
- A record: accessors become `namespace()`/`key()`, next to `getId()` elsewhere in the API, and its canonical constructor is public.

## D2: `of(String)` is the only factory
**Status:** Accepted

**Context:** BD-0003 registers endpoints by `String` id, so parsing is the one path with a caller.

**Decision:** One static factory, `NamespacedId.of(String id)`. An `of(namespace, key)` can be added without breaking anything once a caller has the halves already split.

**Rejected:**
- `of(namespace, key)` now: nothing calls it.

## D3: No direction type; ADR 0018 stops listing one
**Status:** Accepted

**Context:** ADR 0018's Decision says `client-api` holds "its own small value types (namespaced id, direction)", but direction is implicit in `registerIncoming`/`registerOutgoing` (BD-0003).

**Decision:** No direction type. ADR 0018's bullet keeps its point (client-api has its own value types and never reuses `protocol`'s) and names the namespaced id as the example.

**Rejected:**
- Building a direction type to match the ADR's wording: nothing would use it.

## D4: The id rule stays as specified; widening it is BD-0009
**Status:** Accepted

**Context:** The 64-character limit was a placeholder, and Minecraft's identifier character set (`.` in both halves, `/` in the key) came up during planning.

**Decision:** BD-0001 copies today's rule (`protocol-v1.md` § Namespaced Id/Key: `[a-z0-9_-]+:[a-z0-9_-]+`, 64 characters). The character set and the length limit are both decided in BD-0009, which must land before 0.1.

**Rejected:**
- Changing the rule in this issue: it spreads a client-api issue across the spec and `protocol`.

## D5: The limit and pattern live in one place each, and tests derive from them
**Status:** Accepted

**Context:** The rule now exists in `Endpoint` and in `NamespacedId`, and BD-0009 will change both.

**Decision:** `NamespacedId` holds the limit as a package-private `MAXIMUM_LENGTH` constant and the rule as one pattern; `NamespacedIdTest` builds its boundary ids from `MAXIMUM_LENGTH`, so changing the number is one edit.

**Rejected:**
- A public constant: it would put the number into the API contract for no caller.

## D6: Bad format raises `IllegalArgumentException`, `null` raises `NullPointerException`
**Status:** Accepted

**Context:** An id passed to `client-api` is caller input at setup time, not wire input.

**Decision:** A malformed id throws `IllegalArgumentException` with the id in its message (CLAUDE.md § API design decisions, ADR 0011); `null` throws `NullPointerException` from `Objects.requireNonNull`, documented in the Javadoc. Uppercase is rejected, not lowercased.

**Rejected:**
- `ProtocolException`: it is the wire-input exception, and `client-api` can't see `protocol`.

## D7: Javadoc is checked by hand in this issue
**Status:** Accepted

**Context:** "Every public type has Javadoc" needs a check, and the consistent Javadoc pattern itself is not decided yet.

**Decision:** The acceptance check is `./gradlew :bend-client-api:javadoc` with no "no comment" warning. The pattern and any build enforcement are BD-0010.

**Rejected:**
- doclint as errors wired into `check` now: build tooling ahead of BD-0010's decision and the open ADR 0015.

## D8: Lombok only in non-API modules
**Status:** Accepted

**Context:** `protocol` uses Lombok (`Endpoint`'s `@Getter`), so it is easy to assume `client-api` may too, if only as `compileOnly`.

**Decision:** Lombok is for `protocol` and the `-netty` modules; an API module (`client-api`, later `server-api`) never uses it, not even `compileOnly`. Each public member is written out so it carries its own Javadoc (ADR 0021), and the source reads exactly like the API.

**Rejected:**
- `compileOnly` Lombok in `client-api`: generated members have no Javadoc, and the sources jar shows annotations instead of the methods callers use.
