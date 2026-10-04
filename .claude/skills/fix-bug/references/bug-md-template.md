# BUG.md template

Save to `docs/issues/<branch-name>/BUG.md`, where `<branch-name>` is the exact current branch (`BD-NNNN-<kebab-slug>`), no suffix stripping.

## What to fill when

- **Phase 5 (now):** Problem, Reproduction Steps, Status: `Investigating`.
- **Phase 6:** Root Cause (Status → `Diagnosed`), then Chosen Approach once the developer picks a candidate.
- **Phase 7:** Fix (Status → `Fixing`).
- **Phase 8:** Verification (filled by the `bug-fix-verifier` agent; `runtime-integrity-reviewer`'s report is appended below it as `### Runtime integrity`). Status → `Verified` after PASS.
- **Notes / Follow-ups:** any time something adjacent surfaces. Empty is fine.

## Template

```markdown
# BD-NNNN: <short bug title>

**Date:** <YYYY-MM-DD>
**Issue:** [BD-NNNN](link to issue)
**Status:** Investigating | Diagnosed | Fixing | Verified | Abandoned

## Problem
<2-4 sentences on the observable defect. Pulled from the issue's Problem
section, restated in your own words so this file reads independently. Name
the protocol-v1.md rule it violates, if any.>

## Reproduction Steps
<Use exactly one of the headings below, matching the path you took.>

**Failing test (3a):** `protocol/src/test/java/net/onestorm/bend/protocol/codec/VarInt28FrameDecoderTest.java#<method>`
Run with: `./gradlew :bend-protocol:test --tests 'net.onestorm.bend.protocol.codec.VarInt28FrameDecoderTest.<method>' --rerun`

**or**

**Diagnosis evidence (3b):**
- Stack trace / error log line: `<pasted trace>`
- Or concrete file:line reference: `<path:line>`
- Regression test that will ship with the fix: `<path/to/FooTest.java#<method>>`
  Run with: `./gradlew :bend-<module>:test --tests '<fqcn>.<method>' --rerun`

## Root Cause
<Filled in Phase 6. 2-4 sentences on why the defect happens. Cite file:line
for the code at fault. Describe the mechanism, not the symptom.>

## Chosen Approach
<Filled at the end of Phase 6 once the developer accepts a candidate.
One line naming the picked solution. Alternatives stay in chat, not here.>

## Fix
<Filled in Phase 7. What changed, in what files, why that addresses the
root cause. Not a diff dump: a short explanation a future reader can grasp
without opening the PR.>

## Verification
<Filled by bug-fix-verifier in Phase 8. Score, verdict, evidence.
runtime-integrity-reviewer's `### Runtime integrity` block follows it:
findings and checked, verbatim. Empty until the agents write it.>

## Notes / Follow-ups
<Adjacent issues, missing tests elsewhere, refactors worth doing later.
Empty is fine.>
```
