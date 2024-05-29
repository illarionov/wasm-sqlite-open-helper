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

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.android")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.android-instrumented-test")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_main_version",
    envVariableName = "WSOH_SQLITE_MAIN_VERSION",
).get()

android {
    namespace = "ru.pixnews.wasm.sqlite.open.helper"
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
            implementation(projects.native.sqliteAndroidWasmEmscriptenIcu346)
            implementation(projects.native.sqliteAndroidWasmEmscriptenIcuMtPthread346)
        }

        commonMain.dependencies {
            api(projects.commonApi)
            api(projects.sqliteCommon)
            api(projects.sqliteDatabasePathResolver)
            api(projects.sqliteException)
            implementation(projects.commonCleaner)
            implementation(projects.commonLock)
            implementation(projects.wasiEmscriptenHost)
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
