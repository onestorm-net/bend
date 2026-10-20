# Quality gates: input (Phase 1.5), surface (Phase 1.6), and output (Phase 4d)

`/plan-feature` runs three fail-closed gates that bracket the drafting work. The tables themselves live in SKILL.md so they fire reliably. This document carries the rationale, sycophancy guards, and operating notes; load it when you reach a gate.

## Why three gates

The path of least resistance ("this looks crisp, let me draft and we can iterate") is the most expensive failure mode in planning. A draft built on partial information costs a full restart later (see `anti-patterns.md`). The input gate makes "is the input complete?" a binary question instead of a feel. The surface gate catches what convention checks don't see (untrusted bytes sizing an allocation, a peer that goes silent, a blocked event loop, a leaked buffer, spec drift). The output gate catches the symmetric failure: drafted with thin filler text in the gaps.

## Phase 1.5: input gate

**The rule:** every ✓ requires a quoted source. **No ? or ✗ row may survive into drafting**: Phase 2 exists to close them, so an open row routes you into interrogation rather than blocking you where you stand. Phase 3 is the step that's unreachable while any row is still ? or ✗. Fail-closed.

Output the eight-row table verbatim to the developer with marks and citations filled in. It stays in the conversation; see below.

**Phase 1.6 runs next in both branches; an all-✓ gap table does not exempt a feature from the surface section.** After it:

- **All ✓**: say "All eight checks pass with cited evidence. Surface section written. Proceeding to Phase 3 confirmation." Skip Phase 2 interrogation; the input is genuinely complete.
- **Any ? or ✗**: list the missing rows and proceed to Phase 2 to fill them. Phase 2 questions target only ? and ✗ rows. Don't sweep.

### Sycophancy guards

- A ✓ without a quoted source is invalid. If you can't cite it, mark ?.
- An issue body of "Add keep-alive to the client" cannot mark Acceptance Criteria ✓: there's nothing testable in there.
- A long developer prompt is not evidence of completeness. Length doesn't make a row ✓; only specifics do.
- "Edge cases will be handled during implementation" is ✗, not ✓.

The table is not persisted into PLAN.md. `plan-reviewer` grades Key Design Decisions, module shape, layer fit and the acceptance criteria directly, and the implementing session needs the sections the table proves complete, not the proof. What the gate produces is the ✓ marks; what PLAN.md carries is the content they vouch for.

## Phase 1.6: security & cost surface gate

**The rule:** seven rows. For each row, the planner writes a **prose answer to the row's questions** ending in a Proof line, or marks `N/A: <one-line reason>`. Empty cells are not allowed; paraphrasing the questions back is not an answer. Fail-closed.

Question-shaped, not field-shaped, by design. A field-shaped gate ("list every packet, list every handler") trains the planner to fill in a form for the past; questions transfer to the next feature.

### Sycophancy guards specific to the surface gate

These flag prose that *looks* like an answer but doesn't actually answer the row's questions:

- **"Netty handles this"** without naming the class and method that does it (or "verify at implementation") is THIN. Name the handler, the base class, the method.
- **"The frame cap bounds it"** under Row 1 is THIN for anything inside a frame. `maximumFrameLength` bounds the frame; an array count inside a 4MB frame can still claim two billion elements. Name the check that runs before the allocation.
- **"We validate it"** is THIN unless the answer says where (encode, decode, construction) and with which exception type.
- **"There's a timeout"** under Row 2 is THIN; name its value, the spec rule behind it, the mechanism that fires it, and what cancels it on close.
- **Walking only the happy path.** Row 2 and Row 7 ask for the failure walk (silent peer, disconnect mid-flow, overlapping updates, in-flight message on a removed endpoint). An answer that only describes the clean handshake or the clean unregister is THIN; the failures are the whole point of the row.
- **"It's single-threaded"** under Row 3 is THIN unless the answer names what confines it (one event loop per channel) and checks every public method callable from a user thread.
- **"Netty releases it"** under Row 4 is THIN unless it names the base class or call that releases, and covers the failure path, not only the success path.
- **"We always do that" / "It's a convention"** under Row 6 is THIN. The row's question is *which enforcement level* holds the rule. Tribal-knowledge defences are L4 by default: name that explicitly with a trade-off, or escalate to L1/L2.
- **Silent failure toward the library user.** Under Row 2 and Row 4, "it's logged" is not a signal the caller can act on. If a connection dies, a write fails or a message is dropped, name what the API caller observes (a future completing exceptionally, a disconnect callback, a thrown exception).
- **False N/A on Row 1.** A feature that adds or changes anything decoded from the wire cannot mark Row 1 N/A. Search the Approach for packet classes, `decode` methods, `ProtocolUtil` readers and codec changes before accepting it.
- **False N/A on Row 7.** A plan that touches the endpoint registry, routing or `Available`/`Supported Endpoints` handling cannot mark Row 7 N/A.
- **A Proof line with no case in it.** "Covered by the existing suite", "a test will assert it" and a case name that does not exist are THIN. So is a case that replays the answer: a threading proof on an `EmbeddedChannel`, a timeout proof that never advances time.

## Phase 4d: output gate

**The rule:** for each row in the SKILL.md output table, mark **OK** if the bullet rule is met **OR** if the section explicitly declares `N/A: <one-line reason>` (e.g. "Documentation Sync, `protocol-v1.md`: N/A, no wire or rule change"). Otherwise **THIN** and re-work the section before continuing.

The N/A carve-out exists because some plans legitimately have nothing in a section (a `client-netty`-internal refactor has no spec or Javadoc change), and treating those as THIN would block them. Sweep Inventory is different: with no content the heading is omitted entirely, not marked N/A. An explicit N/A with a reason is substance; silent emptiness or "TBD" is not.

### Sycophancy guards specific to the output

- "TBD" in any row is THIN, not OK. If you don't know yet, you shouldn't be at Phase 4.
- A section consisting of one bullet is THIN unless the bullet rule explicitly allows it.
- Acceptance Criteria written as goals ("the client should handle disconnects well") are THIN. Each row is a binary observable.
- Restating the Goal in different sections doesn't count toward those sections' bulk.

If any row is THIN, fix the section using evidence from Phase 1.5 / Phase 2 transcripts. Do not invent new content here; that's a sign the input gate let something slip and you should re-open Phase 2 for that topic.

When every row is OK, proceed to Phase 5.
