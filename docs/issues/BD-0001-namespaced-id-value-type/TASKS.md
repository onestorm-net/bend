# BD-0001: NamespacedId value type, zero-dependency module

> Plan: [PLAN.md](/docs/issues/BD-0001-namespaced-id-value-type/PLAN.md) | Decisions: [DECISIONS.md](/docs/issues/BD-0001-namespaced-id-value-type/DECISIONS.md)

### Phase 1: NamespacedId (2 tasks)

**Goal:** `client-api` callers get a validated, immutable endpoint id type that fails when created, not on the wire.

**Phase Context:**

- Why NOT reuse `protocol`'s `Endpoint`: `client-api` depends on nothing (ADR 0018), and `Endpoint` throws `ProtocolException`, the wire-input exception. A bad id from an API caller is caller misuse, so `IllegalArgumentException` (D6).
- Why NOT a record: `get` accessors match the rest of the API (D1); the worked example in the task-writer skill uses a record, this plan deliberately doesn't.

- [ ] **1.1** Add `NamespacedId` to `client-api`, with its build setup and the rules it introduces
    - **Context:**
        - **Why:** Endpoint ids reach the API as strings; an id the protocol rejects should fail at `NamespacedId.of`, not later on the wire. See D1, D2, D6.
        - **Architecture:** `public final class NamespacedId` in `net.onestorm.bend.client.api`. Private constructor `(namespace, key)`, `static of(String id)`: `Objects.requireNonNull(id, "id")`, then `id.length() > MAXIMUM_LENGTH` → `IllegalArgumentException("Namespaced id \"" + id + "\" exceeds maximum length " + MAXIMUM_LENGTH)`, then `PATTERN.matcher(id).matches()` with `PATTERN = ([a-z0-9_-]+):([a-z0-9_-]+)`, groups 1 and 2 are namespace and key; no match → `IllegalArgumentException` naming the id and the expected form. `getNamespace()`, `getKey()`, `asString()` (`namespace + ":" + key`), `toString()` returns `asString()`, `equals`/`hashCode` over both fields. Package-private `static final int MAXIMUM_LENGTH = 64` (D5). Rule from `protocol-v1.md` § Namespaced Id/Key, unchanged (D4).
        - **Key refs:**
            - `protocol/src/main/java/net/onestorm/bend/protocol/endpoint/Endpoint.java:16-36`: the limit, the pattern, length-before-regex order and message shape to mirror (not call)
            - `protocol/src/test/java/net/onestorm/bend/protocol/endpoint/EndpointTest.java:61-80`: invalid-name cases and the `assertThatThrownBy` style
            - `protocol/src/test/java/net/onestorm/bend/protocol/packet/DisconnectPacketTest.java`: `@ParameterizedTest` precedent
            - `protocol/build.gradle.kts`: `testImplementation(libs.assertj.core)`
            - `docs/decisions/0018-client-built-by-implementation-builder.md`: Decision, third bullet (D3)
        - **Watch out:**
            - No Lombok and no jspecify in `client-api`, not even `compileOnly` (D8); only `testImplementation(libs.assertj.core)` is added
            - `matches()`, not `find()`: `"a:b:c"` must be rejected, not split
            - The 64 limit is on the full `namespace:key` string, colon included
            - Uppercase is rejected, not lowercased
            - `null` gives `NullPointerException` (from `requireNonNull`), not `IllegalArgumentException`
            - Tests build boundary ids from `MAXIMUM_LENGTH`, never a literal 64 (D5), so BD-0009 changes one constant
            - Javadoc on every public member, including `equals`, `hashCode`, `toString`, plus `package-info.java`
    - **Scope:** The value type, its tests, Javadoc and package doc; the `client-api` test dependency; the ADR 0018 wording (D3) and the "no Lombok in API modules" lines in `CLAUDE.md` and `client-api/CLAUDE.md` (D8). Wiring ids into registration is BD-0003.
    - **Acceptance Criteria:** AC #1, #2, #3, #4, #5
    - **Touches:**
        - `client-api/build.gradle.kts`
        - `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`
        - `client-api/src/main/java/net/onestorm/bend/client/api/package-info.java`
        - `client-api/src/test/java/net/onestorm/bend/client/api/NamespacedIdTest.java`
        - `docs/decisions/0018-client-built-by-implementation-builder.md`
        - `CLAUDE.md`, `client-api/CLAUDE.md`
    - **Action items:**
        - [GREEN] Add `dependencies { testImplementation(libs.assertj.core) }` to `client-api/build.gradle.kts` (scaffolding so the tests compile)
        - [RED] `of_validId_exposesParts`, `of_allAllowedCharacters_accepted`
        - [RED] `of_exactlyMaximumLength_accepted`, `of_overMaximumLength_throws`
        - [RED] `of_invalidFormat_throws` (`@ParameterizedTest` over `""`, `":"`, `"a:"`, `":b"`, `"ab"`, `"a:b:c"`, `"A:b"`, `"a:B"`, `"a b:c"`, `"a.b:c"`, `"a:b/c"`, `"é:b"`), `of_null_throws`
        - [RED] `equals_sameValue_equalWithSameHashCode`, `equals_differentValue_notEqual`
        - [GREEN] Implement `NamespacedId` with Javadoc on the class (rule, spec section, immutability, thread safety), `of` (`@throws` for both exceptions), accessors and overrides
        - [GREEN] Add `package-info.java` with package Javadoc
        - [GREEN] ADR 0018 Decision bullet 3: "holds interfaces and its own small value types, such as the namespaced id; it never reuses `protocol`'s types"
        - [GREEN] `CLAUDE.md` § API design decisions and `client-api/CLAUDE.md`: the D8 line, linking `/docs/issues/BD-0001-namespaced-id-value-type/DECISIONS.md`
    - **Verify before complete:**
        - [ ] `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.NamespacedIdTest' --rerun` passes
        - [ ] No Lombok/jspecify: `./gradlew :bend-client-api:dependencies --configuration compileClasspath` and `--configuration runtimeClasspath` both print "No dependencies"
        - [ ] `matches()` not `find()`: `"a:b:c"` case in `of_invalidFormat_throws`
        - [ ] Full-string limit: `of_exactlyMaximumLength_accepted` / `of_overMaximumLength_throws`, both built from `MAXIMUM_LENGTH`
        - [ ] Uppercase rejected: `"A:b"` and `"a:B"` cases
        - [ ] Null: `of_null_throws` asserts `NullPointerException`
        - [ ] Every invalid case in `EndpointTest` (no colon, uppercase, too long) has a counterpart in `NamespacedIdTest`
        - [ ] Javadoc: `./gradlew :bend-client-api:javadoc` prints no "no comment" warning (first confirm JDK 25 javadoc reports missing comments by default; if not, run it once with `-Xdoclint:all`)
        - [ ] ADR 0018 bullet 3 no longer names direction; both CLAUDE.md files carry the D8 line with a root-relative link and no em dash
    - **Success:** `NamespacedId` validates exactly like `Endpoint`, compares by value, is fully documented, and `client-api` still has no main dependencies

- [ ] **1.2** Final verification
    - **Verify:**
        - [ ] `./gradlew build` passes
        - [ ] JaCoCo gap check: `client-api/build/reports/jacoco/test/jacocoTestReport.xml` shows no `<line>` with `mi > 0` in `NamespacedId.java`; any remaining gap traced to "is this reachable at all", then tested or deleted
    - **Success:** Full build green, `NamespacedId` fully covered or each gap explained

## Review Notes

**Reviewed:** 2026-10-20

### Coverage
- **Acceptance criteria:** 5/5 covered (all in 1.1; AC 1's build-file read and the full build also in 1.2)
- **Planned files:** 7/7 appear in some task's `**Touches:**`

### Gaps
- *(None)*

### Scope Drift
- *(None)*
