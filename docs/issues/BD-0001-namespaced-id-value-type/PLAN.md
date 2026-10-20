# BD-0001-namespaced-id-value-type: NamespacedId value type, zero-dependency module

**Date:** 2026-10-20
**Status:** Approved
**Issue:** [BD-0001](https://onestorm.kendo.dev/projects/1/issues/BD-0001)
**Decisions:** [DECISIONS.md](/docs/issues/BD-0001-namespaced-id-value-type/DECISIONS.md)

## Goal
A client author can name an endpoint only with an id the protocol accepts, because `client-api`'s
`NamespacedId` refuses every other id when it is created.

## Key Design Decisions

| Decision | Choice | Convention Reference |
|----------|--------|---------------------|
| Type shape | `public final class NamespacedId`, private constructor, `getNamespace()`, `getKey()`, `asString()`; `toString()` returns `asString()`; `equals`/`hashCode` over namespace and key (D1) | `get` accessors as in `protocol/src/main/java/net/onestorm/bend/protocol/endpoint/Endpoint.java` (Lombok `@Getter`: `getId()`, `getName()`) |
| Construction | `public static NamespacedId of(String id)` only (D2) | Static factory named `of`, as `Endpoint#of`; one-argument because callers (BD-0003, planned) register by `String` id |
| The rule | Length check `id.length() > MAXIMUM_LENGTH` (64) first, then one pattern `([a-z0-9_-]+):([a-z0-9_-]+)` whose two groups are the namespace and key (D4) | `Endpoint#MAXIMUM_NAME_LENGTH`, `Endpoint#NAMESPACED_ID_PATTERN`; `protocol/protocol-v1.md` § Namespaced Id/Key |
| Limit constant | `private static final int MAXIMUM_LENGTH = 64`; the test declares its own `64` from the spec (D5) | `Endpoint#MAXIMUM_NAME_LENGTH` is `private` too |
| Bad input | `IllegalArgumentException` naming the id; `null` → `NullPointerException` via `Objects.requireNonNull` (D6) | CLAUDE.md § API design decisions (ADR 0011: caller misuse at setup time is `IllegalArgumentException`) |
| Package | `net.onestorm.bend.client.api`, with a `package-info.java` | Package used by the pre-migration client-api; mirrors `net.onestorm.bend.protocol.*` |
| Dependencies | `client-api/build.gradle.kts` adds only `testImplementation(libs.assertj.core)`; JUnit comes from the root `testing.suites` block | `client-api/CLAUDE.md` ("Depends on nothing"), `build.gradle.kts` (`useJUnitJupiter`), `protocol/build.gradle.kts` (`testImplementation(libs.assertj.core)`) |
| No Lombok | Accessors, `equals`, `hashCode` written by hand (D8) | "Depends on nothing" excludes `compileOnly(libs.lombok)` too; D8 makes "no Lombok in API modules" an explicit rule |

**Layer compliance:** `NamespacedId` lives in `client-api` and uses only `java.lang`,
`java.util.Objects` and `java.util.regex`. `client-api/build.gradle.kts` declares no main
dependencies, and CLAUDE.md § API design decisions requires exactly that.

**Module shape:**

| Class | Inputs/outputs | Hidden behaviour | Test seam | Shallow-test reckoning | Verdict |
|---|---|---|---|---|---|
| `NamespacedId` | `of(String)` → id; three `String` accessors | The spec rule (length, character set, single colon), splitting into halves | Create from strings, assert accessors, equality, rejections | A second use case (ids already split into halves) adds a factory method `of(namespace, key)`. That's the normal growth path for a value type, not one method per use case. Exempt per the lens (value class) | Shallow-but-justified (value type) |

## Scope
- **In scope:**
  - `client-api/build.gradle.kts`: test dependency only
  - `client-api/src/main/java/net/onestorm/bend/client/api/package-info.java`
  - `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`
  - `client-api/src/test/java/net/onestorm/bend/client/api/NamespacedIdTest.java`
  - `docs/decisions/0018-client-built-by-implementation-builder.md`: the "(namespaced id, direction)" wording (D3)
  - `CLAUDE.md` and `client-api/CLAUDE.md`: one line each, no Lombok in API modules (D8)
- **Out of scope:**
  - A direction type (D3)
  - `of(namespace, key)`, `Comparable`, `Serializable` (D2)
  - Changing the id rule: the character set and the length are BD-0009 (D4)
  - Javadoc enforcement in the build (BD-0010, D7)
  - Any change to `protocol` or `protocol/protocol-v1.md`

## Approach

1. **ADR 0018** (`docs/decisions/0018-client-built-by-implementation-builder.md`, Decision, third
   bullet): "holds interfaces and its own small value types (namespaced id, direction)" becomes
   "holds interfaces and its own small value types, such as the namespaced id; it never reuses
   `protocol`'s types". Record the change under D3 (already written).
   **CLAUDE.md** (§ API design decisions): add "Lombok is for `protocol` and the `-netty` modules
   only; an API module never uses it, not even `compileOnly`. (BD-0001 D8,
   [DECISIONS.md](/docs/issues/BD-0001-namespaced-id-value-type/DECISIONS.md))". **`client-api/CLAUDE.md`**:
   add "No Lombok, not even `compileOnly`: public members are written out so each carries its own
   Javadoc." with the same link.
2. **`client-api/build.gradle.kts`**: add a `dependencies { testImplementation(libs.assertj.core) }`
   block under the existing comment line.
3. **`NamespacedIdTest`** first (TDD), then **`NamespacedId`**:
   - `of(id)`: `Objects.requireNonNull(id, "id")`; if `id.length() > MAXIMUM_LENGTH`, throw
     `IllegalArgumentException("Namespaced id \"" + id + "\" exceeds maximum length " + MAXIMUM_LENGTH)`;
     `PATTERN.matcher(id)` and test it with `matches()` (whole string, as `Endpoint` does; not
     `find()`); on no match, throw `IllegalArgumentException` naming the id and the expected
     form; otherwise `new NamespacedId(group(1), group(2))`.
   - `asString()` returns `namespace + ":" + key`. `toString()` returns `asString()`.
   - `equals`/`hashCode`: both fields (`Objects.hash(namespace, key)`).
   - Javadoc on the class (the rule, quoting the spec section, immutability, thread safety), on
     `of` (with `@throws` for both exceptions) and on each accessor, `equals`, `hashCode` and
     `toString`.
4. **`package-info.java`**: package Javadoc ("Public contract for code that uses a Bend client").
5. Run `./gradlew :bend-client-api:test --rerun`, `./gradlew :bend-client-api:javadoc` and
   `./gradlew build`.

## Security & Cost Surface

### Untrusted wire input and allocation bounds
`NamespacedId` reads nothing off the wire. Its input is the caller's own string, passed at
registration time (BD-0003), so no peer-sized value reaches it in this issue. The length check runs
before the regex, mirroring `Endpoint`'s constructor, so an over-long caller string never reaches
the matcher. The rejection message embeds the whole id, as `Endpoint`'s does. That string is the
caller's own, so its size is the caller's cost, never a peer's. One hazard to note for later: if `client-netty` ever builds a
`NamespacedId` from a name decoded off the wire, a bad name would raise `IllegalArgumentException`
instead of `ProtocolException`. That cannot happen with names that already passed `Endpoint`'s
constructor, which enforces the same rule; see Risks. Proof: `NamespacedIdTest#of_overMaximumLength_throws`.

### Connection lifecycle, timeouts and silent peers
N/A: a value type with no connection, state or waiting.

### Threading and the event loop
`NamespacedId` is immutable: two `private final String` fields set in the private constructor, no
setters, and a `static final Pattern` (which is thread-safe, while each `Matcher` is local to `of`).
Instances can be shared freely across caller threads and the event loop. The class Javadoc states
this. Enforcement is L4 (review), chosen deliberately: a reflection test asserting `private final`
fields, or a concurrency test of `of`, would test the language rather than the class (ADR 0009).
Proof: the field declarations in `NamespacedId.java` (only `private final` instance fields), checked at PR review.

### Resource ownership
N/A: no buffers, handles or per-connection state. An instance holds two short strings.

### Spec conformance
The rule touched is `protocol-v1.md` § Namespaced Id/Key: "a namespace string and a key string,
both lowercase, a-z, 0-9, dashes/underscores", "separated by a colon", "maximum 64 characters". The
spec text doesn't change. `protocol` enforces it on decode and on `Endpoint.of` (raising
`ProtocolException`), and `client-api` enforces the same rule on `NamespacedId.of` (raising
`IllegalArgumentException`). Wire compatibility doesn't change. The new copy adds no check the
spec doesn't call for, and no check is unreachable: each branch (null, too long, no pattern match)
has a test feeding it input. The two copies agreeing is enforced only at L4: no test can compare
them across modules, since `client-api` can't depend on `protocol`. The matching test cases show
they agree today, and BD-0009 changes both together. Proof: `NamespacedIdTest#of_invalidFormat_throws`
includes every invalid input `EndpointTest` has.

### Public API surface, module boundaries and enforcement
The new public type is `net.onestorm.bend.client.api.NamespacedId`, with `of`, `getNamespace`,
`getKey`, `asString`, `equals`, `hashCode` and `toString`. There are no Netty types. Module
dependencies: `client-api` gains one test-only dependency (AssertJ). The rule that it has no main
dependencies is enforced at compile time (L1): any `io.netty` or `net.onestorm.bend.protocol`
import fails compilation, because nothing on the compile classpath provides them. The two send
paths aren't touched. Caller misuse throws `IllegalArgumentException` for a bad format and
`NullPointerException` for `null`, both documented. The Javadoc requirement is enforced by review
(L4), checked with `./gradlew :bend-client-api:javadoc`. Build enforcement is BD-0010.
Proof: `./gradlew :bend-client-api:dependencies --configuration compileClasspath` and `--configuration runtimeClasspath` each print "No dependencies" (`compileClasspath` is the one that would show a `compileOnly` Lombok or jspecify).

### Runtime endpoint add and remove
N/A: registration and removal are BD-0003. This issue only provides the id type they key on.

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| 1 | `client-api` declares no dependency other than test dependencies | Read `client-api/build.gradle.kts`; `./gradlew :bend-client-api:dependencies --configuration compileClasspath` and `--configuration runtimeClasspath` both show none |
| 2 | Every id that breaks the spec format (including every invalid case `EndpointTest` has: no colon, uppercase, too long) is rejected by `of` with `IllegalArgumentException` naming the id; `null` throws `NullPointerException` | `NamespacedIdTest` (`of_invalidFormat_throws`, `of_overMaximumLength_throws`, `of_null_throws`) |
| 3 | ADR 0018's Decision no longer lists a direction type among client-api's value types (D3), and both CLAUDE.md files state no Lombok in API modules (D8) | Read `docs/decisions/0018-client-built-by-implementation-builder.md` (Decision, third bullet), `CLAUDE.md` § API design decisions and `client-api/CLAUDE.md` |
| 4 | A valid id exposes namespace, key and full form; equal values are `equals` with the same `hashCode`, different values are not equal | `NamespacedIdTest` (`of_validId_exposesParts`, `equals_sameValue_equalWithSameHashCode`, `equals_differentValue_notEqual`) |
| 5 | Every public type and member has Javadoc | `./gradlew :bend-client-api:javadoc` prints no "no comment" warning (verify at implementation time that JDK 25's javadoc reports missing comments by default; if not, run it once with `-Xdoclint:all` for the check) |

## Reuse and patterns

- `protocol/src/main/java/net/onestorm/bend/protocol/endpoint/Endpoint.java`: mirror: length-before-regex order, the pattern, the 64 limit and the shape of its error messages (not called: `client-api` can't depend on `protocol`)
- `protocol/src/test/java/net/onestorm/bend/protocol/endpoint/EndpointTest.java`: mirror: the invalid-name cases and the `assertThatThrownBy(...).isInstanceOf(...).hasMessageContaining(...)` style
- `protocol/src/test/java/net/onestorm/bend/protocol/packet/DisconnectPacketTest.java`: mirror: `@ParameterizedTest` use in this codebase
- `protocol/build.gradle.kts`: mirror: `testImplementation(libs.assertj.core)`

## Testing Strategy

One PR. Run: `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.NamespacedIdTest' --rerun`

| Test | Behaviour | Fails when |
|---|---|---|
| `of_validId_exposesParts` | `of("bend:chat")` gives namespace `bend`, key `chat`, `asString()` and `toString()` `bend:chat` | the split is off by one, or the halves are swapped |
| `of_allAllowedCharacters_accepted` | `of("az09_-:az09_-")` is accepted, with its parts intact | an allowed character class (digit, `_` or `-`) is missing from the pattern |
| `of_exactlyMaximumLength_accepted` | An id of exactly 64 characters (the test's own `MAXIMUM_LENGTH`, `"a".repeat(MAXIMUM_LENGTH - 2) + ":b"`) is accepted | the check is `>=` instead of `>`, or the class's limit is below 64 |
| `of_overMaximumLength_throws` | 65 characters throws `IllegalArgumentException` containing "maximum length" | the length check is missing, or the class's limit is above 64 |
| `of_invalidFormat_throws` (`@ParameterizedTest`, `@ValueSource`) | `""`, `":"`, `"a:"`, `":b"`, `"ab"`, `"a:b:c"`, `"A:b"`, `"a:B"`, `"a b:c"`, `"a.b:c"`, `"a:b/c"`, `"é:b"` each throw `IllegalArgumentException` containing the id | the pattern allows an empty half, a second colon, uppercase, a space, `.`, `/` or non-ASCII |
| `of_null_throws` | `of(null)` throws `NullPointerException` | null reaches `length()` with a less useful message, or is turned into `IllegalArgumentException` |
| `equals_sameValue_equalWithSameHashCode` | Two `of("bend:chat")` instances are equal with equal hash codes | `equals`/`hashCode` are left as identity |
| `equals_differentValue_notEqual` | `bend:chat` vs `bend:chat2` and vs `other:chat` are not equal | `equals` compares only one half |
| `equals_otherTypeOrNull_notEqual` | `equals("bend:chat")` and `equals(null)` return `false` | `equals` casts without an `instanceof` check |

## Documentation Sync

| Source change | Document | Update / N/A |
|---|---|---|
| Wire format, packet fields, limits, MUST/disconnect rules, flow | `protocol/protocol-v1.md` | N/A: the rule is copied, not changed (D4) |
| New or changed public type or method in `client-api` | Javadoc on that type | Update: `NamespacedId` and `package-info.java` |
| A design decision, layer rule, convention or module plan | `CLAUDE.md` / ADR | Update: ADR 0018 Decision wording (D3); a "no Lombok in API modules" line in `CLAUDE.md` § API design decisions and in `client-api/CLAUDE.md` (D8). The other rules used ("depends on nothing", `IllegalArgumentException` for caller misuse, Javadoc in API modules) are already there |

## Edge Cases
- Empty string, `":"`, an empty namespace (`":b"`) or key (`"a:"`): no pattern match, so `IllegalArgumentException`.
- No colon (`"ab"`) or two colons (`"a:b:c"`): `:` isn't in either character class, so `IllegalArgumentException`.
- Uppercase: rejected, not lowercased (D6).
- `.` and `/`: rejected under today's rule. BD-0009 flips these cases to accepted.
- Non-ASCII (`é`): rejected. Since every allowed character is ASCII, `length()` (UTF-16 units) equals the character count for every id that can pass.
- Exactly 64 characters: accepted. 65: rejected with "maximum length".
- `null`: `NullPointerException` with the message `id`.

## Risks
- **Two copies of one rule drift.** `Endpoint` and `NamespacedId` hold the rule separately, because `client-api` may not depend on `protocol`. The guard is the matching test cases (AC 3) and BD-0009, which changes both copies together. If they ever diverge, a client could create an id the server's `Endpoint` rejects, or the reverse.
- **Wire names converted later.** When `client-netty` maps `Available Endpoints` names onto registrations, it should compare against registered `NamespacedId`s (`asString()`) or convert only names `Endpoint` already validated. Calling `NamespacedId.of` on raw wire input would surface a peer's error as `IllegalArgumentException`. That's for the client-netty epic; this note is the hand-off.
- **Javadoc check strength.** If JDK 25's javadoc doesn't report missing comments by default, AC 5's command proves nothing. Check this when implementing (see AC 5).
