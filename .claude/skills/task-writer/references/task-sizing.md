# Task sizing: when to bundle, when to split

The default for `/task-writer` is **coarser-grained tasks**. One task should
own a coherent module end-to-end: a `protocol` change (packet class + codec
registration + `protocol-v1.md` section + tests) is *one* task, not three.
Splitting at intra-module boundaries creates handoffs without reducing risk:
the model holds a full module coherently, and the reviewer reads commits,
not tasks.

This file holds the decision rules that drive sizing.

## Sizing matrix

| Feature size | Files | Task count | How to split |
|---|---|---|---|
| Small | 1-5 | 1 task + final verification | Don't split |
| Medium | 6-15 | 1-2 tasks + final verification | By module, in dependency order |
| Large | 15+ | 2-4 tasks + final verification | By PR, module boundary, or risk gate |

Every TASKS.md ends with the final verification task, whatever its size.

**Why bigger than you'd expect:** splitting one module into separate
"types" / "logic" / "tests" tasks used to hedge against context loss
mid-module. That hedge is no longer load-bearing. Keep splits where they buy
something real (a module boundary another module depends on, a reviewable PR
boundary, a human checkpoint before a risky change).

## Default cross-module split

Bend's modules depend on each other in one direction:
`protocol` → `client-api` → `client-netty` / `server-netty` → round-trip
tests. A feature that spans them splits along that order:

```
Phase N: [Feature Name] (3-4 tasks)

- [ ] N.1 protocol end-to-end (TDD)
      → Packet classes, ProtocolUtil readers/writers, codec + registration changes
      → protocol-v1.md changes in the same task as the code that enforces them
      → Tests in protocol/src/test/java, JUnit Jupiter + AssertJ, method_condition_expected names
      → Success: ./gradlew :bend-protocol:test --rerun passes

- [ ] N.2 client-api end-to-end (TDD)
      → Interfaces, value types, Javadoc on every public type and member
      → No Netty types in public signatures (java.nio.ByteBuffer at the boundary)
      → Success: ./gradlew :bend-client-api:test --rerun passes

- [ ] N.3 client-netty / server-netty end-to-end (TDD)
      → Pipeline handlers, send paths, registration/routing, disconnect handling
      → Success: ./gradlew :bend-client-netty:test --rerun (or :bend-server-netty) passes

- [ ] N.4 Final verification
      → ./gradlew build passes
      → JaCoCo XML gap check for every touched class
```

Skip the rows for modules the feature doesn't touch. A `client-api`-only
feature is one implementation task plus final verification.

## Approval gates: what triggers a new task

Create separate tasks **only** at these boundaries. If a split doesn't fall
on one of these, it's probably artificial; collapse it.

1. **Risk boundary**: a wire-format change (packet layout, frame format,
   VarInt width), a new disconnect condition, or anything where you want a
   human checkpoint *before* proceeding
2. **PR boundary**: when the plan ships as multiple PRs. One phase per PR
3. **Module boundary**: `protocol` → `client-api` → `-netty` is a real
   handoff; the downstream module compiles against the upstream one. Keep
   each module as one coherent task
4. **Architectural decision needed**: multiple valid approaches, need user
   input before continuing
5. **Final verification**: `./gradlew build` plus the JaCoCo gap check always
   gets its own final task

## Splitting one task further

When the default per-module split isn't enough:

- A `-netty` task is large enough that one commit would be hard to review →
  split at the handler boundary (`N.3a` pipeline/codec wiring + `N.3b`
  registration/routing)
- A wire-format change is risky to get wrong → checkpoint the `protocol`
  task (spec + codec) for human approval before the implementations build
  on it
- Architectural uncertainty in one module → isolate that module so the
  decision is reviewable on its own

## Not a reason to split

- "This touches a lot of files." One coherent task can own a full module.
- "The spec change is its own thing." It isn't; spec and enforcing code
  travel together so they can't drift.
- "I want progress to feel granular." Tasks are not check-ins; they're units
  of integration.
- "The reviewer might miss something." The pre-PR reviewers read the whole
  branch diff, not the task list; task granularity doesn't change what they
  see.

## Decision framework

| Question | Answer |
|---|---|
| Small change (1-5 files)? | One implementation task + final verification |
| Medium feature (6-15 files)? | 1-2 tasks by module + final verification |
| Large cross-module feature (15+)? | 2-4 tasks, split at PR / risk / module boundary |
| Touches `protocol-v1.md`? | Same task as the code that implements it |
| Wire-format change? | Checkpoint the `protocol` task for approval |
| Architectural uncertainty? | Stop and ask before implementing |
