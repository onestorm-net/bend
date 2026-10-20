# 0014: Gradle build layout

**Status:** Accepted

## Context

Bend moved from Maven to Gradle. The layout follows Velocity's own build
(https://github.com/PaperMC/Velocity, `settings.gradle.kts` and the root `build.gradle.kts`).

Two IDE and DSL problems shaped the details. A Gradle `group` ending in `.bend`, combined with
`rootProject.name = "bend"`, made IntelliJ show `bend.bend` in every module name, because it
builds module names as `<group>.<rootProject.name>.<subproject>.<sourceSet>`. And Kotlin DSL only
generates the typed `java { }` / `testing { }` accessors for a script whose own top-level
`plugins { }` block applies `java-library`.

Older Gradle versions cannot run under a JDK 25 host JVM at all: Gradle 8.8's Kotlin DSL
compiler fails to parse `25.0.3` as a version. A locally installed Gradle can't be relied on.

## Decision

- Gradle with the Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`), multi-module, with
  dependency versions in `gradle/libs.versions.toml`.
- Folders are short and unprefixed (`protocol/`, `client-api/`, `client-netty/`,
  `server-netty/`); Gradle projects carry the prefix (`:bend-protocol`, `:bend-client-api`, ...),
  mapped in `settings.gradle.kts`. The published artifact id then matches the old Maven
  artifact ids with no `base.archivesName` config.
- Gradle `group` is `net.onestorm`, not `net.onestorm.bend`. Java packages (`net.onestorm.bend.*`)
  are unaffected.
- The root `build.gradle.kts` has a top-level `plugins { java-library }` block (for accessors)
  and applies plugins to subprojects with `apply<PluginType>()` inside `subprojects { }`, since
  the `plugins { }` form is rejected there. Subprojects get a JDK 25 toolchain, `mavenCentral()`
  and JUnit Jupiter through the JVM Test Suite DSL
  (`testing.suites.named<JvmTestSuite>("test") { useJUnitJupiter(...) }`), which also manages
  the JUnit Platform launcher.
- The JUnit version comes from the catalog through the stable `VersionCatalogsExtension` API
  (`rootProject.the<VersionCatalogsExtension>().named("libs").findVersion("junit-jupiter")`).
- The Gradle wrapper is pinned to 9.7.1. Always run `./gradlew`, never a bare `gradle`.

## Does not decide

- Publishing configuration (repositories, signing). Not set up yet.

## Rejected

- Group `net.onestorm.bend`: puts the project name in the group, and causes the IDE stutter.
- Prefixed folder names: the same stutter, on disk.
- The generated `libs.versions.junitJupiter` accessor inside `subprojects { }`: it did not
  resolve there (cause unknown); the stable API always works.
- `tasks.withType<Test> { useJUnitPlatform() }`: needs manual launcher wiring per module.
