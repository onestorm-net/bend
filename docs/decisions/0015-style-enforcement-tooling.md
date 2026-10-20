# 0015: Style enforcement tooling

**Status:** Open
**Date:** 2026-09-17

## Context

Two tools were tried and removed from the build on 2026-09-17:

- **Spotless** (`palantirJavaFormat`, `removeUnusedImports()`, whitespace cleanup) was wired
  into `check`. It is a formatter: it rewrites code instead of failing the build, while the goal
  was a failing build so a violation gets reviewed and fixed deliberately.
- **Checkstyle** was chosen as a report-only companion but never added. Its flagship rule, a
  private field never reassigned outside the constructor that could be `final`, has no
  Checkstyle check; the closest, `VisibilityModifier`
  (https://checkstyle.sourceforge.io/checks/design/visibilitymodifier.html), checks field
  visibility, not finality. PMD has this rule (`ImmutableField`).

A custom regex-based `checkNoWildcardImports` Gradle task was removed on the same day, for the
same reason a patchwork of one-off tasks was not wanted.

## Decision

This ADR binds nothing in 0.1. No style tool is in the build. What is known about the tool to
pick:

- One single plugin, not a formatter plus custom tasks plus a linter.
- Violations fail the build rather than being rewritten.
- It should have compiler or type-level context (in the spirit of Error Prone), not only
  textual or syntactic matching.
- Reports as XML or CSV, not HTML (as with [ADR 0010](0010-coverage-reporting.md)); config as
  plain, readable files in the repo (e.g. `config/<tool>/`).
- The motivating rule is `final` on private fields never reassigned outside the constructor.

## Does not decide

- Which tool. Candidates noted: PMD, or a narrower Checkstyle ruleset built from checks it does
  have (`VisibilityModifier`, `FinalClass`, `HiddenField`).
- Whether any rule beyond field finality is enforced.

## Rejected

- Spotless as the enforcement tool: rewrites instead of failing.
- Checkstyle for the field-finality rule: has no such check.
- Custom regex Gradle tasks (`checkNoWildcardImports`): no type information, and one more piece
  of patchwork.
- Adding `final` to every never-reassigned local and parameter (e.g. OpenRewrite's
  `FinalizeLocalVariables` / `FinalizeMethodArguments`): a contested style that Google's Java
  Style Guide deliberately doesn't require; often noise rather than signal. Private fields are
  the different, more valuable case.
