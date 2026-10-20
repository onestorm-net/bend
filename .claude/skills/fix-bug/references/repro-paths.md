# Repro paths

Phase 3 of `/fix-bug` produces **something executable that demonstrates the defect**. That's what Phase 8's verifier checks against. Two valid paths, plus a pre-flight validation step and a hard "cannot reproduce" stop condition.

## Before picking a path: validate the issue's contract

Read the issue's *Expected behaviour* / *Steps to reproduce* section against the code it touches and against `protocol/protocol-v1.md`. The reporter writes in terms of what they saw; the spec and the code encode the contract. Common gaps:

- A bullet says "X should be accepted" but `protocol-v1.md` says a reader MUST disconnect on X (e.g. a frame whose length decodes to `0`, more than 16 consecutive NUL padding bytes). The bullet may be a request to change the spec, not a bug.
- A bullet uses a name that resembles a code identifier but means something different to the reporter (e.g. "available endpoints" meaning "what I registered", while `AvailableEndpointsPacket` is what the server advertises).
- A bullet contradicts a recorded ruling in `CLAUDE.md` or an `Accepted` ADR (e.g. asking for non-canonical VarInts to be rejected, which ADR 0003 considered and deliberately declined).
- Two bullets contradict each other under realistic usage.

If you spot any of these, **do not write failing tests yet**. Surface the contradiction to the developer in plain prose and ask which model is canonical:

> The issue says *"X should be accepted"*, but protocol-v1.md's Frame section requires a disconnect on X. Is this a spec change, or the reporter's symptom-language?

Only after the developer confirms the canonical model, pick a path and write tests. Otherwise the failing tests become contractual on a contract the developer never signed off on, and Phase 6 has to walk back the assertions.

## 3a. Failing test first (default)

Write a JUnit Jupiter + AssertJ test that fails on HEAD for the exact reason the issue reports, then passes after the fix. Name it `method_condition_expected` and put it in the existing test class for the unit under test (`protocol/src/test/java/net/onestorm/bend/protocol/...`). Use this when the mechanism is fuzzy: writing the test forces you to pin down the expected behaviour before patching.

Netty pipeline bugs are reproduced with `io.netty.channel.embedded.EmbeddedChannel`: write the hostile bytes inbound (`writeInbound`), split them across reads to hit the partial-read paths, and assert on what comes out, what throws, and whether the channel closed. Races and timeouts get the same treatment, made deterministic: drive the event loop by hand (`runPendingTasks`, `advanceTimeBy` on the embedded channel), or coordinate real threads with `CountDownLatch` / `CyclicBarrier` so the bad interleaving happens every run. A test that only fails sometimes is not a repro.

The same test becomes the regression gate in Phase 8. Once the fix makes it green, the bug cannot silently come back.

**BUG.md "Reproduction Steps" entry:**
> **Failing test (3a):** `protocol/src/test/java/net/onestorm/bend/protocol/codec/VarInt28FrameDecoderTest.java#zeroLengthFrame_nonCanonicalEncoding_throws`
> Run with: `./gradlew :bend-protocol:test --tests 'net.onestorm.bend.protocol.codec.VarInt28FrameDecoderTest.zeroLengthFrame_nonCanonicalEncoding_throws' --rerun`

`--rerun` is required: without it Gradle can report an UP-TO-DATE test task and the "pass" proves nothing.

## 3b. Test alongside fix (clear diagnosis)

When the diagnosis is already done *for* you (stack trace pointing at a line, visible typo, null deref staring at you in a recent diff), a failing-test dance is ceremony. Skip it and ship the regression test in the same commit as the fix.

**Use only if all of these hold:** the issue includes a stack trace / error log / concrete file:line OR the defect is a self-evident typo/off-by-one/missing null-check; the fix is mechanical (no design judgment); the regression test ships with the fix (not before).

In BUG.md's Reproduction Steps, record diagnosis evidence in place of a failing test:

> **Diagnosis evidence:** `NullPointerException` at `ClientPacketHandler.java:58`, thrown when a `KeepAlive` arrives before the handshake completed and the session field is still null. No reproduction ceremony; fix + regression test ship together.

Path 3b also skips the hypothesis-ranking sub-step in Phase 6 and the post-mortem prompt in Phase 10: the trace already tells you the cause and the diagnosis didn't surface a structural smell.

## Cannot reproduce at all

Stop. Ask for more context (the bytes on the wire, which side sent what, packet order, timing, configured `maximumFrameLength`). Don't proceed on "probably this".
