# Example: client-api feature

A small single-module feature showing the task format with TDD ordering,
exact Gradle commands, the final verification task, and completion metadata.
Reference this when you need a worked example of how the SKILL.md phases
produce a real TASKS.md.

## Input: the issue

> **BD-0012: NamespacedId value type in client-api.** Endpoint ids in the
> public API are plain `String`s today, so a typo or an uppercase id only
> fails once it hits the wire (`Endpoint`'s constructor throws
> `ProtocolException`). Add a `NamespacedId` value type to `client-api` that
> validates on construction: `namespace:key`, each half lowercase
> `a-z0-9_-`, at most 64 characters total, `IllegalArgumentException` on
> invalid input, equality by value, full Javadoc.

PLAN.md acceptance criteria:

1. `NamespacedId.of("bend:chat")` returns an id with `namespace()` `"bend"` and `key()` `"chat"`
2. Invalid input (missing colon, empty half, uppercase, other characters, over 64 chars, null) throws `IllegalArgumentException`
3. Two ids built from the same string are `equals` and share a `hashCode`; `toString()` returns `namespace:key`
4. Every public type and member has Javadoc

## Output: TASKS.md

```markdown
# BD-0012: NamespacedId value type in client-api

> Plan: [PLAN.md](PLAN.md) | Decisions: [DECISIONS.md](DECISIONS.md)

### Phase 1: NamespacedId (2 tasks)

**Goal:** `client-api` callers get a validated endpoint id type that fails at construction, not on the wire.

**Phase Context:**

- Why NOT reuse `protocol`'s `Endpoint`: it is a wire type (numeric id + flags) that throws `ProtocolException`, which is reserved for untrusted network input. A bad id passed by an API caller is caller misuse, so `IllegalArgumentException` (see D1).

- [ ] **1.1** Add `NamespacedId` to `client-api`
    - **Context:**
        - **Why:** Endpoint ids are strings in the public API, so invalid ids surface late as a wire-side `ProtocolException`. See D1.
        - **Architecture:** A `public record NamespacedId(String namespace, String key)` with a compact constructor that validates each half, plus `static NamespacedId of(String id)` that splits on the single `:`. Records give value equality for free. Rules mirror `protocol-v1.md` and `Endpoint`'s regex exactly.
        - **Key refs:**
            - `protocol/src/main/java/net/onestorm/bend/protocol/endpoint/Endpoint.java:16-18`: `MAXIMUM_NAME_LENGTH` and `NAMESPACED_ID_PATTERN`, the rules to mirror
            - `protocol/src/test/java/net/onestorm/bend/protocol/endpoint/EndpointTest.java`: test style to match (AssertJ, `method_condition_expected`)
            - `CLAUDE.md` § API design decisions: no Netty types in `client-api`
        - **Watch out:**
            - The 64-character limit applies to the full `namespace:key` string, not to each half
            - `"a:b:c"` has two colons and must be rejected, not split into `a` and `b:c`
            - Use `IllegalArgumentException`, not `ProtocolException`; `client-api` must not throw a wire exception for caller misuse
            - `client-api` must not gain a dependency on `protocol` or Netty just for this type
    - **Scope:** The value type, its validation, Javadoc, and its unit tests. Wiring it into `Client` registration methods is a later issue.
    - **Acceptance Criteria:** AC #1, #2, #3, #4
    - **Touches:**
        - `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`
        - `client-api/src/test/java/net/onestorm/bend/client/api/NamespacedIdTest.java`
    - **Action items:**
        - [RED] `of_validId_splitsIntoNamespaceAndKey`
        - [RED] `of_missingColon_throws`, `of_twoColons_throws`, `of_emptyNamespace_throws`, `of_emptyKey_throws`
        - [RED] `of_uppercaseCharacter_throws`, `of_disallowedCharacter_throws`, `of_null_throws`
        - [RED] `of_exactly64Characters_accepted`, `of_65Characters_throws`
        - [RED] `equals_sameString_isEqualWithSameHashCode`, `toString_returnsNamespaceColonKey`
        - [GREEN] Implement `NamespacedId` as a record with compact-constructor validation and `of(String)`
        - [GREEN] Javadoc the type, both components, `of`, and `toString`
    - **Verify before complete:**
        - [ ] `./gradlew :bend-client-api:test --tests 'net.onestorm.bend.client.api.NamespacedIdTest' --rerun` passes
        - [ ] 64-char limit is on the full string: `of_exactly64Characters_accepted` / `of_65Characters_throws`
        - [ ] Two colons rejected: `of_twoColons_throws`
        - [ ] Exception type is `IllegalArgumentException` in every invalid-input test
        - [ ] `client-api/build.gradle.kts` has no new `protocol` or Netty dependency
    - **Success:** `NamespacedId` validates, compares by value, and is fully documented

- [ ] **1.2** Final verification
    - **Verify:**
        - [ ] `./gradlew build` passes
        - [ ] JaCoCo gap check: `client-api/build/reports/jacoco/test/jacocoTestReport.xml` shows no `<line>` with `mi > 0` in `NamespacedId.java`; any remaining gap traced to "is this reachable at all", then tested or deleted
    - **Success:** Full build green, `NamespacedId` fully covered

## Review Notes

**Reviewed:** 2026-10-04

### Coverage
- **Acceptance criteria:** 4/4 covered
- **Planned files:** 2/2 appear in some task's `**Touches:**`

### Gaps
- *(None)*

### Scope Drift
- *(None)*
```

## After `/next` completes 1.1

```markdown
- [x] **1.1** Add `NamespacedId` to `client-api`
    ...existing content...
    - **Completed:** 2026-10-05
    - **Learnings:**
        - A record's compact constructor runs for `new NamespacedId(ns, key)` too, so validation lives there, not only in `of`
        - Checking the length before running the regex keeps the error message specific ("exceeds 64 characters" rather than "invalid characters")
    - **Key Changes:**
        - Created `client-api/src/main/java/net/onestorm/bend/client/api/NamespacedId.java`: record with validation and Javadoc
        - Created `client-api/src/test/java/net/onestorm/bend/client/api/NamespacedIdTest.java`: 11 tests
```

## What this example illustrates

- **A small issue still gets a TASKS.md**: one implementation task plus the final verification task
- **Context block enables task pickup after `/clear`**: a fresh session can read Why / Architecture / Key refs / Watch out and resume without re-reading the plan
- **Watch out items have corresponding verification checks**: every "Watch out" entry maps to a `Verify before complete` row
- `[RED]` tests come **before** `[GREEN]` implementation, and the tests are named `method_condition_expected`
- The `IllegalArgumentException` vs `ProtocolException` choice is captured in **Watch out** and the Phase Context, not buried in implementation prose; it's a decision a reviewer needs to see
- Verification names the exact targeted Gradle command with `--rerun`; the full `./gradlew build` and JaCoCo gap check live in the final verification task
