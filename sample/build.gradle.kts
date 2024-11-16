@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":jetstream"))
    implementation(platform("org.slf4j:slf4j-bom:2.0.16"))
    implementation("org.slf4j:slf4j-api")
    implementation("org.slf4j:slf4j-simple")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            testType = TestSuiteType.UNIT_TEST
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events = TestLogEvent.values().toSet() - TestLogEvent.STARTED
        exceptionFormat = TestExceptionFormat.FULL
    }
    outputs.upToDateWhen { false }
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(project.javaToolchains.launcherFor(java.toolchain))
}

application {
    mainClass = "com.lopcode.bluesky.sample.SampleRunner"
    applicationDefaultJvmArgs = listOf("-Xmx1024m")
}