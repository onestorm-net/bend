# Pre-completion verification

Every implementation task in TASKS.md needs a structured "Verify before
complete" block. This file holds the patterns and the rationale.

The point: replace vague "tests pass" success criteria with explicit checks
that map back to the task's `Watch out` items. Every "Watch out" entry must
appear in the verification block; otherwise the edge case is documented but
not enforced.

## Verification template

Add to each implementation task:

```markdown
- **Verify before complete:**
    - [ ] All [RED] action items have passing tests
    - [ ] All [GREEN] action items implemented
    - [ ] `./gradlew :bend-<module>:test --tests '<fqcn>' --rerun` passes
    - [ ] Each "Watch out" edge case addressed:
        - [ ] [specific edge case 1]: handled by [how / test name]
        - [ ] [specific edge case 2]: handled by [how / test name]
    - [ ] Integration point works: [specific check]
```

Always write the exact Gradle command, with the Gradle project path
(`:bend-protocol`, `:bend-client-api`, `:bend-client-netty`,
`:bend-server-netty`, never the folder name) and `--rerun`, so an
UP-TO-DATE test task can't report a pass without running anything. javac is
the type check: a targeted test run that compiles is the type check for that
module.

## Good vs bad

**Bad (vague):**

```markdown
- **Success:** Tests pass
```

**Good (explicit):**

```markdown
- **Verify before complete:**
    - [ ] `./gradlew :bend-protocol:test --tests 'net.onestorm.bend.protocol.codec.VarInt28FrameDecoderTest' --rerun` passes
    - [ ] Edge cases from Watch out:
        - [ ] `0x80 0x00` (non-canonical zero length) → ProtocolException (test: `nonCanonicalTwoByteZeroLength_smallBufferPath_throws`)
        - [ ] 3 continuation bytes with the 4th not yet arrived → waits, no throw (test: `fourByteLength_deliveredOneByteAtATime_stillDecodes`)
    - [ ] Integration: `protocol-v1.md` § Frame states the `Length == 0` MUST
- **Success:** Zero-length frames rejected, partial reads still wait, spec matches
```

## Verification by task type

| Task type | Verification focus |
|---|---|
| `protocol` | Targeted `:bend-protocol` test run; encoder/decoder round-trip for every new field; hostile-input tests (oversized/negative length prefixes); spec text matches the code |
| `client-api` | Targeted `:bend-client-api` test run; Javadoc on every public type and member; no Netty types in public signatures |
| `client-netty` / `server-netty` | Targeted test run for the module; disconnect paths tested; no blocking on the event loop; every `ByteBuf` released on success and on exception |
| Round-trip | Client and server exchange the new traffic end-to-end, including the disconnect case |
| Final verification | `./gradlew build` plus the JaCoCo XML gap check for touched classes |

## Edge case cross-check: non-negotiable

Every item in **Watch out** MUST appear in **Verify before complete**:

```markdown
- **Watch out:**
    - Array length prefix is attacker-controlled; allocate only after checking readable bytes
    - Encoder must refuse what the decoder can't read back (width asymmetry)
    - Built-in packets must not go through the public endpoint send path

- **Verify before complete:**
    - [ ] Oversized length prefix: `readVarIntArray_lengthExceedsAvailableBytes_throwsWithoutAllocating` passes
    - [ ] Width symmetry: `encode_lengthOverConfiguredMaximum_throws` passes
    - [ ] Send path: `HandshakePacket` written via the internal path (test asserts it reaches the channel)
```

If a Watch out has no matching verification, you've documented a risk you're
not enforcing. Either add the check, or remove the Watch out; leaving it
hanging is worse than not flagging it at all.

## Review happens once per branch, not per task

`/next` does **not** spawn reviewer agents; per-task review was retired
because first-pass scores were 9-10 nearly every time. The gate for a task is
its verification: the targeted test run (which compiles, so javac is the type
check).

Review runs once against the whole branch via `/review-branch`, which spawns
`runtime-integrity-reviewer`, `correctness-reviewer` and `precedent-reviewer`.
**Don't list them as verification steps in a task**: they don't run at task
granularity, and the defects they hunt (ByteBuf leaks across handlers, sibling
drift between client and server) only become visible once the branch is
whole.

## The final verification task

Bend has no CI, no git hooks, and no linter or formatter, so nothing runs the
full build for you. Every TASKS.md therefore ends with a final verification
task:

- `./gradlew build` (all modules, all tests, javac across the board)
- A JaCoCo XML gap check for every class the branch touched: open
  `<module>/build/reports/jacoco/test/jacocoTestReport.xml`, find the class's
  `<sourcefile>`, and look for `<line>` elements with `mi` greater than 0
  (the CSV next to it gives a quick per-class summary first)

For every uncovered line the check finds, the question is "is this reachable
at all", not "how do I turn it green". Reachable → write a real test.
Unreachable (a defensive check the current code can't produce) → delete it.
Never write a contrived reflection test to cover it. There is no coverage
threshold gate in the build; the gap check is the gate.

Don't add separate lint or format rows; Bend has no such tooling yet.

## Self-check before finalising TASKS.md

This checklist **is** the Phase 4 gate; nothing downstream re-checks it. Walk
every row before showing TASKS.md to the developer:

### Structure

- [ ] Is each task a complete logical unit (not just one file)?
- [ ] Do action items list `[RED]` tests BEFORE `[GREEN]` implementation?
- [ ] Does each task have clear success criteria?
- [ ] Are approval gates only at natural boundaries?
- [ ] Is the task count appropriate for the feature size?
- [ ] Does TASKS.md end with the final verification task?

### Context completeness

- [ ] Would a developer know when they're "done"?
- [ ] Can this task be picked up after `/clear`? (Context block complete?)
- [ ] Do Key refs point at real files and lines (`file:line`), not just module names?

### Verification quality

- [ ] Does each task have a "Verify before complete" section?
- [ ] Does each name the exact `./gradlew :bend-<module>:test --tests '...' --rerun` command?
- [ ] Are verification checks explicit (not just "tests pass")?
- [ ] Does every "Watch out" item have a corresponding verification check?
- [ ] Are specific test names (`method_condition_expected`) or `file:line` references included?

### Acceptance Criteria coverage

- [ ] Does every implementation task have an `**Acceptance Criteria:**` line?
- [ ] Does every AC from PLAN.md appear in at least one task's AC mapping?
- [ ] Are "(partial)" annotations used when a task only covers part of an AC?
- [ ] Do scaffolding tasks note "None directly" for their AC mapping?

### Plan coverage

- [ ] Every file from plan appears in Touches?
- [ ] Every disconnect condition / hostile-input case captured in Watch out or tests?
- [ ] Every `protocol-v1.md` change in the same task as its enforcing code?
- [ ] Every verification step in the final verification task?
- [ ] No items from plan "fell through the cracks"?

If any row is unchecked, fix it before handing off. No agent audits this
afterwards, so an unchecked row here is a gap that reaches implementation.
Record the coverage counts in TASKS.md's `## Review Notes` (see Phase 4).
