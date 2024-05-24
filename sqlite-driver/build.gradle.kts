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
    id("com.google.devtools.ksp")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.android")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.atomicfu")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_driver_version",
    envVariableName = "WSOH_SQLITE_DRIVER_VERSION",
).get()

android {
    namespace = "ru.pixnews.wasm.sqlite.driver"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
                testTask.useJUnitPlatform()
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
        }
        androidUnitTest.dependencies {
            implementation(libs.androidx.test.core)
            implementation(libs.kermit.jvm)
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.params)

            implementation(projects.sqliteEmbedderGraalvm)
            implementation(projects.sqliteEmbedderChasm)
            implementation(projects.sqliteEmbedderChicory)
            implementation(projects.native.sqliteAndroidWasmEmscriptenIcu345)
            implementation(projects.native.sqliteAndroidWasmEmscriptenIcuMtPthread345)

            runtimeOnly(libs.junit.jupiter.engine)
        }

        commonMain.dependencies {
            api(projects.commonApi)
            api(projects.sqliteCommon)
            api(projects.sqliteDatabasePathResolver)
            api(projects.sqliteException)
            api(libs.androidx.sqlite.sqlite)
            implementation(projects.commonCleaner)
            implementation(projects.commonLock)
            implementation(projects.wasiEmscriptenHost)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.testing)
        }

        val jvmAndAndroid by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(projects.commonLock)
            }
        }
        androidMain.get().dependsOn(jvmAndAndroid)
        jvmMain.get().dependsOn(jvmAndAndroid)
    }
}

dependencies {
    testAnnotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
}