# BD-0008: Old CLAUDE.md to new destinations

One row per line of `grep -nE '^#{1,4} |^\s*- \*\*' CLAUDE.md` on the pre-BD-0008 file (68
lines), plus rows marked `+` for decisions that sit in plain bullets. Line numbers refer to that
file. Local-only facts are recorded as "dropped: local reference" and never quoted here.

Destination legend: `ADR NNNN`; `root` (root CLAUDE.md line, under the named heading); module
name (that module's CLAUDE.md); `dropped: status (<Kendo ref>)`; `dropped: local reference`;
`title` (the file's title only).

| Line | Old content | Destination | Done |
|---|---|---|---|
| 1 | File title and purpose | title; root intro | [ ] |
| 8 | § Status | dropped: status (epics 2 to 6) | [x] |
| 10 | Protocol design done; runtime endpoint add/remove is core protocol | ADR 0016 (core-protocol line); rest dropped: status | [x] |
| 13 | Gradle migration done | dropped: status (fact visible in the build) | [x] |
| 15+ | client-api / client-netty reset to skeletons, old code kept elsewhere | dropped: status (BD-0001 to BD-0006); dropped: local reference | [x] |
| 22+ | server-netty not started | dropped: status (epic 4) | [x] |
| 24+ | No git history yet | dropped: status (no longer true) | [x] |
| 27 | § Frame decoder | ADRs 0001 to 0007; protocol | [ ] |
| 34 | 21-bit width vs 4MB limit bug | ADR 0001 (Context) | [x] |
| 43 | Rename to VarInt28, 28-bit reader, why 28 | ADR 0001 | [x] |
| 60+ | Frame cap enforced by explicit length check, not by VarInt width | ADR 0001 | [x] |
| 66+ | readRawVarIntSmallBuf resets and waits instead of truncating | ADR 0001 (Context) | [x] |
| 78 | `length < 0` check removed as unreachable | ADR 0009 (Context) | [x] |
| 99 | `isReadable()` guard removed as unreachable | ADR 0009 (Context) | [x] |
| 110 | maximumFrameLength constructor arg, clamp, no no-arg constructor, encoder enforces | ADR 0001; protocol | [ ] |
| 130 | NUL padding skip, 16-byte limit | ADR 0002; protocol | [ ] |
| 144 | Zero-length frame rejected | ADR 0003; protocol | [ ] |
| 156 | Canonical VarInt encoding declined | ADR 0003 (Rejected) | [x] |
| 171+ | Audit documentation: Length width, exact-Length fields, Packet ID one byte, VarInt byte order | ADR 0001 (Length width, byte order); ADR 0003 (fields, Packet ID) | [x] |
| 181 | Endpoint direction bit and id validation | ADR 0005; protocol | [ ] |
| 192 | Array pre-allocation bug | ADR 0004 (Context) | [x] |
| 206 | validateArrayLength fix | ADR 0004; protocol | [ ] |
| 212+ | Exposed in both directions | ADR 0004 (Context) | [x] |
| 217 | Spec gaps closed in the audit | ADR 0006 | [x] |
| 237 | Disconnect Type values, `3: Timeout` rename | ADR 0007 | [x] |
| 260 | § Protocol architecture: two layers | ADR 0008; root (Protocol architecture); client-netty | [ ] |
| 276+ | Two send paths in client-netty; old code conflated them | ADR 0008; client-netty | [ ] |
| 288 | § Build tooling | ADR 0014; root (Build tooling) | [ ] |
| 290 | Maven to Gradle, Kotlin DSL, version catalog | ADR 0014 | [x] |
| 294+ | Version 0.1.0-SNAPSHOT set at the root | ADR 0020 | [x] |
| 298 | Folder names vs Gradle project names | ADR 0014; root (Build tooling) | [ ] |
| 315 | Gradle group `net.onestorm` | ADR 0014 | [x] |
| 327+ | Root build script layout, `apply<>()` in subprojects, VersionCatalogsExtension, test suites | ADR 0014 | [x] |
| 358 | Wrapper pinned to 9.7.1, always `./gradlew` | ADR 0014; root (Build tooling) | [ ] |
| 365+ | Public GitHub repo, Kendo tracker | root (where things live) | [ ] |
| 368 | § Code formatting / style enforcement | ADR 0015 | [x] |
| 370 | Spotless and Checkstyle removed, unresolved | ADR 0015 | [x] |
| 378 | Spotless rewrites rather than fails | ADR 0015 (Rejected) | [x] |
| 382 | Checkstyle lacks the field-finality rule | ADR 0015 (Rejected) | [x] |
| 392+ | Next session: evaluate alternatives | ADR 0015 (Does not decide) | [x] |
| 397 | One single plugin; custom wildcard task removed | ADR 0015 | [x] |
| 404 | Tool should have type-level context | ADR 0015 | [x] |
| 411 | `final` on locals/params declined | ADR 0015 (Rejected) | [x] |
| 422+ | Future tool: XML/CSV reports, config in repo | ADR 0015 | [x] |
| 428 | § Testing / coverage | ADRs 0009, 0010; root (Testing / coverage); protocol | [ ] |
| 430 | JaCoCo on every subproject, finalizedBy | ADR 0010 | [x] |
| 435 | XML + CSV, no HTML | ADR 0010; root (Testing / coverage) | [ ] |
| 443 | No coverage gate yet | ADR 0010 | [x] |
| 450+ | Stubborn gap means ask "is this reachable" | ADR 0009; root (Testing / coverage) | [ ] |
| 458 | No defensive checks for unreachable cases | ADR 0009; root (Testing / coverage) | [ ] |
| 472 | § Module naming convention | ADR 0013; root (Module naming convention) | [ ] |
| 476 | API gets the plain name | ADR 0013 | [x] |
| 478 | Implementation gets a transport suffix | ADR 0013 | [x] |
| 485 | § Module set | ADRs 0016, 0017; root (Module set) | [ ] |
| 487 | For 0.1: module list | ADR 0016; root (Module set) | [ ] |
| 497 | Deferred to 0.2+ | ADR 0017 | [x] |
| 498+ | No server-api in 0.1 and why | ADR 0016 | [x] |
| 504 | server-api re-add naming | ADR 0016 (Does not decide) | [x] |
| 510 | Module/plugin system, auto-teardown | ADR 0017 | [x] |
| 522+ | Module loading, hot-swap goal | ADR 0017 | [x] |
| 527 | Enable/disable on live connections | ADR 0017 (D10) | [x] |
| 540+ | Dividing line: core protocol vs module system | ADR 0016; server-netty | [ ] |
| 548+ | Don't half-build loading in 0.1 | ADR 0016 | [x] |
| 554+ | Forward-compatibility: Netty-agnostic server-netty signatures | ADR 0016; server-netty | [ ] |
| 563 | § API design decisions | ADRs 0011, 0012; root (API design decisions) | [ ] |
| 565 | No ByteBuf in public API modules | ADR 0012; root; client-api | [ ] |
| 576 | ByteBuffer at the boundary | ADR 0012; client-api; client-netty | [ ] |
| 581 | ProtocolException replaces IllegalArgumentException for wire input | ADR 0011; root; protocol | [ ] |
| 592 | No stack traces by default | ADR 0011 | [x] |
| 606 | Debug toggle (Lombok accessors on the static field) | ADR 0011 | [x] |
| 614 | § Versioning: first release is 0.x | ADR 0020 | [x] |
| 623 | § Documentation scope: Javadoc for public contract modules, gut-check | ADR 0021; root (Documentation scope) | [ ] |
| 634+ | Docs site and versioned Javadoc hosting | ADR 0024 | [x] |
| 639 | § In-flight work (client) | ADRs 0012, 0018, 0019 | [x] |
| 641 | EndpointCodec known issue, client rework | ADR 0012 (rework target); dropped: status (BD-0002); dropped: local reference | [x] |
| 651+ | Supported ids = available intersect registered | ADR 0019 | [x] |
| 653 | Available endpoints definition | ADR 0019 | [x] |
| 654 | Registered endpoints definition | ADR 0019 | [x] |
| 655+ | Resend triggers; full snapshot, not delta | ADR 0019 | [x] |
| 661 | Client built by implementation builder; reverses ServiceLoader | ADR 0018; client-api; client-netty | [ ] |
| 665 | All configuration outside client-api | ADR 0018 | [x] |
| 670 | ServiceLoader pointless | ADR 0018 (Rejected) | [x] |
| 675 | client-api depends on nothing, owns value types | ADR 0018; client-api | [ ] |
| 679 | Concrete client package-private | ADR 0018; client-netty | [ ] |
| 686+ | Trade-off: constructing code imports client-netty | ADR 0018 | [x] |
| 691 | § Workflow going forward | ADRs 0022, 0023; root (Workflow) | [ ] |
| 693 | Tracker is live | dropped: status (in `.claude/skills/kendo/SKILL.md`); dropped: local reference | [x] |
| 696+ | Commit messages reference issue keys | ADR 0022; root (Workflow) | [ ] |
| 698 | Branching model | ADR 0022; root (Workflow) | [ ] |
| 716 | Keyless branches | ADR 0022; root (Workflow) | [ ] |
| 724 | Epic breakdown, no Protocol epic | ADR 0023 | [x] |
| 734 | Sequencing, no server-api epic before 0.1.0 | ADR 0023 | [x] |
