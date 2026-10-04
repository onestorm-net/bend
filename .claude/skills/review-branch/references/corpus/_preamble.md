# Hunting corpus

What to look for on this branch, by defect class. Your agent file says who you are and how to
report; these sections say what to check. Your agent file names the sections you carry. A
section that does not apply to this diff is reported as not applying in `checked`, with the
reason, never skipped in silence.

Each section is a census where a census is possible: list the items first, then check each
item against the same questions, then cite the line that answers each question.

Sections, in the order a full pass runs them:

| File | Heading | Carried by |
|---|---|---|
| `security-cost-surface.md` | Security & cost surface | `runtime-integrity-reviewer` |
| `runtime-data-flow.md` | Runtime data-flow | `runtime-integrity-reviewer` |
| `contract-boundary.md` | Contract & boundary integrity | `runtime-integrity-reviewer` |
| `runtime-state-simulation.md` | Runtime-state simulation | `correctness-reviewer` |
| `sins-of-omission.md` | Sins of omission | `correctness-reviewer` |
| `test-disciplines.md` | Test disciplines (diff-gated) | `correctness-reviewer` |
| `recorded-rulings.md` | Recorded rulings | `precedent-reviewer` |
| `sibling-precedent.md` | Sibling precedent | `precedent-reviewer` |

One home per rule. A section belongs to exactly one lane, so a rule edited here changes one
reviewer and no other.

The examples in these sections are Bend's: a Netty pipeline (frame decoder, packet decoder,
handlers), the packet classes in `protocol`, the endpoint registry, and the `client-api` /
`server-netty` public surface. Where a shape has no analogue in the diff, say so in `checked`
rather than manufacturing one.
