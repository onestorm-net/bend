plugins {
    `java-library`
    jacoco
}

allprojects {
    group = "net.onestorm"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply<JacocoPlugin>()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    val junitJupiterVersion = rootProject.the<VersionCatalogsExtension>()
        .named("libs")
        .findVersion("junit-jupiter")
        .get()
        .requiredVersion

    testing.suites.named<JvmTestSuite>("test") {
        useJUnitJupiter(junitJupiterVersion)
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            csv.required.set(true)
            html.required.set(false)
        }
    }

}
