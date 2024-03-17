/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    `kotlin-dsl`
}

group = "ru.pixnews.sqlite-wasm-builder"

kotlin {
    explicitApi = ExplicitApiMode.Warning
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit5)
            targets.all {
                testTask.configure {
                    configureTestTaskDefaults()
                }
            }
            dependencies {
                implementation(platform(libs.junit.bom))
                implementation(libs.assertk)
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.params)
            }
        }
        register<JvmTestSuite>("functionalTest") {
            testType = "functional-test"
            useJUnitJupiter(libs.versions.junit5)

            dependencies {
                implementation(gradleApi())
                implementation(gradleTestKit())
                implementation(libs.assertk)
                implementation(libs.assertk)
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.params)
                implementation(platform(libs.junit.bom))
                implementation(project())
            }

            targets.all {
                testTask.configure {
                    configureTestTaskDefaults()
                    shouldRunAfter(test)
                }
            }
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

private fun Test.configureTestTaskDefaults() {
    maxHeapSize = "1512M"
    jvmArgs = listOf("-XX:MaxMetaspaceSize=768M")
    testLogging {
        events = if (providers.gradleProperty("verboseTest").map(String::toBoolean).getOrElse(false)) {
            setOf(
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_ERROR,
                TestLogEvent.STANDARD_OUT,
            )
        } else {
            setOf(TestLogEvent.FAILED)
        }
    }
}
