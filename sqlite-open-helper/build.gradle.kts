/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("at.released.wasm.sqlite.open.helper.gradle.documentation.dokka.subproject")
    id("at.released.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.android")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.android-instrumented-test")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "at.released.wasm-sqlite-driver"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_main_version",
    envVariableName = "WSOH_SQLITE_MAIN_VERSION",
).get()

android {
    namespace = "at.released.wasm.sqlite.open.helper"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = false
            isIncludeAndroidResources = true
            all { testTask ->
                testTask.useJUnit()
                testTask.maxHeapSize = "2G"
                testTask.testLogging {
                    events = setOf(FAILED, PASSED, SKIPPED, STANDARD_ERROR, STANDARD_OUT)
                }
            }
        }
    }
}

kotlin {
    androidTarget()
    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            api(libs.androidx.sqlite.sqlite24)
            implementation(libs.androidx.core)
        }
        androidUnitTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.room.runtime26)
            implementation(libs.androidx.room.testing26)
            implementation(libs.androidx.test.core)
            implementation(libs.kermit.jvm)

            implementation(projects.sqliteTests.sqliteOpenHelperBaseTests)
            implementation(projects.sqliteEmbedderGraalvm)
            implementation(projects.sqliteEmbedderChasm)
            implementation(projects.sqliteEmbedderChicory)
            implementation(libs.wsoh.sqlite.mt)
            implementation(libs.wsoh.sqlite.st)
        }

        commonMain.dependencies {
            api(libs.cassettes.playhead)
            api(projects.sqliteCommon)
            implementation(projects.commonCleaner)
            implementation(libs.wasi.emscripten.host)
            implementation(libs.wasi.emscripten.host.common.util)
            api(libs.androidx.collection)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.assertk)
        }

        val jvmAndAndroid by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmAndAndroid)
        jvmMain.get().dependsOn(jvmAndAndroid)
    }
}

dependencies {
    constraints {
        listOf(
            "at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-348:*",
            "at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-mt-pthread-348:*",
        ).forEach { dependency ->
            testImplementation(dependency) {
                attributes {
                    attribute(
                        KotlinPlatformType.attribute,
                        KotlinPlatformType.jvm,
                    )
                }
            }
        }
    }
}
