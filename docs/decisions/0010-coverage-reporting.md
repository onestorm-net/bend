# 0010: Coverage reported as XML and CSV, no gate yet

**Status:** Accepted

## Context

Coverage reports in Bend are mostly read by an AI or a script looking for real gaps to test,
not by a person browsing a page.

## Decision

- JaCoCo is applied to every subproject from the root `build.gradle.kts` (a top-level
  `plugins { jacoco }` for accessors, `apply<JacocoPlugin>()` inside `subprojects`).
- `tasks.test` is `finalizedBy(jacocoTestReport)`, so every `./gradlew test` leaves a current
  report.
- Reports are XML and CSV; HTML is off. The CSV
  (`<module>/build/reports/jacoco/test/jacocoTestReport.csv`) gives a per-class summary; the
  XML gives per-line `mi`/`ci` counts under each `<sourcefile>`.
- There is no `jacocoTestCoverageVerification` gate.

## Does not decide

- Whether and when to add a coverage gate. The recorded intent is to revisit once more than one
  module has real code; until then, report first and close real gaps.

## Rejected

- HTML reports: built for browsing, not for reading back.
- A gate before coverage was high: it would only have broken the build.
