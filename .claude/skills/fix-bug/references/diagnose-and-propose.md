# Diagnose & propose (Phase 6 mechanics)

Phase 6 in SKILL.md is the orchestrator: explain the bug, get a candidate picked, no shipping code touched. This file holds the mechanics each sub-step uses.

## Generate hypotheses (skip for path 3b)

Before naming a single root cause, generate **3-5 ranked hypotheses** about what's causing the defect. Anchoring on the first plausible idea is the single most common debugging failure: multiple hypotheses force you to actually compare instead of confirming.

Each hypothesis must be **falsifiable**:

> Format: "If `<X>` is the cause, then `<changing Y>` makes the bug disappear / `<changing Z>` makes it worse."

If you cannot state the prediction, the hypothesis is a vibe, so discard or sharpen it.

When the ranking is genuinely ambiguous and the developer is around, surface the ranked list via `AskUserQuestion` before instrumenting, since they often have domain knowledge that re-ranks instantly ("we just changed #3 last week") or know what's already been ruled out. Don't block on it; proceed with your top pick if no answer.

Skip this step entirely for path 3b (stack trace + mechanical fix): the trace already tells you the cause; ranking strawmen wastes effort.

## Pin down the root cause

Read the code paths again with the repro evidence and the hypothesis ranking in hand. Identify the mechanism, not the symptom. Write 2-4 sentences into BUG.md's **Root Cause**, citing `file:line`. If you can't write a coherent root cause, you don't understand the bug yet, so go back to Phase 3 or sharpen your hypotheses.

Update BUG.md Status to `Diagnosed`.

## Debug instrumentation hygiene

If a hypothesis needs probes (extra output, dumping a value, breakpoint inspection), prefer a probe that touches no shipping code: an assertion in the failing test, `ByteBufUtil.prettyHexDump(buf)` printed from the test, or `jshell --class-path protocol/build/classes/java/main` for pure logic. One inspection beats ten log lines. Bend has no logging framework wired in, so when you do add temporary output to main code, use `System.err.println` and **tag it** with a fresh 4-hex prefix per investigation:

```java
System.err.println("[DEBUG-a4f2] decode length=" + length + " readerIndex=" + buffer.readerIndex());
System.err.println("[DEBUG-a4f2] readable before packet decode: " + ByteBufUtil.hexDump(buffer));
```

Cleanup at the end is then a single `grep -rn '\[DEBUG-a4f2\]' --include='*.java' .`: every hit is a leak. Untagged debug output survives into PRs; tagged output dies together. Run the grep before committing and before Phase 8. Any leaked `[DEBUG-` in the diff is obvious regression noise to the verifier and to human reviewers.

## Decompose the fix surface

Before writing candidates, list the **independent sub-concerns** the fix has to address. For each, classify:

- **Settled**: every reasonable implementer would do the same thing. One line, no debate. (e.g. "release the buffer on the new early-return": trivial regardless of the bigger decision.)
- **Ambiguous**: multiple defensible behaviours exist; the developer needs to pick. (e.g. "on an unknown endpoint id, disconnect, or resend `Available Endpoints` as the spec permits?")

Only **ambiguous** sub-concerns go through the candidate matrix below. Settled sub-concerns are stated up front as "we're going to do X, anyone object?" and implemented in Phase 7 without ceremony.

Bundling settled sub-concerns into the matrix makes the trivial part look hard and dilutes the actual decision. If your candidate list contains entries that all agree on most points, decompose first.

## Explain and propose

Post a chat message with this shape:

> **Bug:** <one-sentence symptom restatement>
> **Root cause:** <2-3 sentences on the mechanism, citing file:line>
> **Why now:** <if relevant: recent commit, edge case, race>

Then lay out every reasonable fix. Each candidate gets:

- **Name**: short label (e.g. "Guard at the boundary", "Fix the typo")
- **Change**: one sentence on what code moves
- **Trade-off**: what this approach costs (scope, complexity, risk)

Three rules:

1. **Always propose at least one**, even when obvious, name it explicitly so the developer can object before code changes.
2. **Don't invent fake alternatives.** Bugs with one good answer (typo, off-by-one, missing null check) get one candidate: "Single candidate: the inverted comparison at `VarInt28FrameDecoder.java:87` is the only place that branch is reachable." Don't pad with strawmen to look thorough.
3. **Recommend one** when there are 2+ real candidates, with a one-line why.

Cap candidates at 4. More than that means the diagnosis isn't tight enough, so go back to root cause.

For Path 3b (stack-trace + mechanical fix), this collapses to a single short message; don't ceremonialise a typo into a four-paragraph proposal.

## Get acceptance via AskUserQuestion

Present as a clickable question; never ask the developer to type. Use `AskUserQuestion` with one option per candidate.

- **Single candidate:** "Proceed with this fix" / "No, let's discuss"
- **Multiple:** one option per candidate, plus "Other / discuss"

Keep labels short (a few words): the trade-offs already live in the chat message above the question, so labels don't need to repeat them.

If the developer picks "discuss" or "Other", iterate the proposal and re-ask. Don't start coding. If the diagnosis itself was wrong, update Root Cause before re-asking.

**If a question is rejected with a clarify-request** (i.e. the developer dismisses the AskUserQuestion entirely rather than picking an option): drop the AskUserQuestion shape on the next turn. Ask in plain prose: *"What would you like to clarify about the candidates?"* The rejection signal means the question *framing* is wrong: re-posing with extra options or rearranged labels usually makes it worse. Two consecutive AskUserQuestion rejections is a strong signal the diagnosis itself drifted; return to Root Cause and re-pin before asking again.

Once a candidate is picked, write a one-liner into BUG.md's **Chosen Approach**, then proceed to Phase 7.
