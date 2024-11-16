@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.net.URI

plugins {
    `java-library`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.slf4j:slf4j-bom:2.0.16"))
    implementation("org.slf4j:slf4j-api")
    implementation("com.github.luben:zstd-jni:1.5.6-7")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
    sourceCompatibility = JavaVersion.VERSION_21 // intentionally kept at 21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Test> {
    testLogging {
        events = TestLogEvent.values().toSet() - TestLogEvent.STARTED
        exceptionFormat = TestExceptionFormat.FULL
    }
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            testType = TestSuiteType.UNIT_TEST
        }

        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            testType = TestSuiteType.INTEGRATION_TEST

            dependencies {
                implementation(project(":jetstream"))
                implementation("org.openjdk.jmh:jmh-core:1.37")
                annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

val githubVersion = System.getenv("GITHUB_VERSION") ?: null
val mavenVersion = githubVersion?.removePrefix("v") ?: "local"
val releasePath = "repos/release-${githubVersion}"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.lopcode.bluesky"
            artifactId = "bluesky-jvm-jetstream"
            version = mavenVersion

            from(components["java"])

            pom {
                name = "bluesky-jvm-jetstream"
                description = "Tools to work with Bluesky's Jetstream system"
                url = "https://github.com/lopcode/bluesky-jvm"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        name = "lopcode"
                        url = "https://github.com/lopcode"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/lopcode/bluesky-jvm.git"
                    developerConnection = "scm:git:https://github.com:lopcode/bluesky-jvm.git"
                    url = "https://github.com/lopcode/bluesky-jvm/tree/main"
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI.create("https://maven.pkg.github.com/lopcode/bluesky-jvm")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "Local"
            url = uri(layout.buildDirectory.dir(releasePath))
        }
    }
}

val signingKey = System.getenv("SIGNING_KEY_ID") ?: null
val signingKeyPassphrase = System.getenv("SIGNING_KEY_PASSPHRASE") ?: null

if (!signingKey.isNullOrBlank()) {
    project.ext["signing.gnupg.keyName"] = signingKey
    project.ext["signing.gnupg.passphrase"] = signingKeyPassphrase
    project.ext["signing.gnupg.executable"] = "/usr/local/bin/gpg"

    signing {
        useGpgCmd()
        sign(publishing.publications)
    }
}