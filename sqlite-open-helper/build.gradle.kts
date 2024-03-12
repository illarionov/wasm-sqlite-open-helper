/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

plugins {
    id("ru.pixnews.sqlite.open.helper.gradle.multiplatform.android")
    id("ru.pixnews.sqlite.open.helper.gradle.multiplatform.kotlin")
}

android {
    namespace = "ru.pixnews.sqlite.open.helper"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
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
                testTask.maxHeapSize = "1G"
                testTask.testLogging {
                    events = setOf(FAILED, PASSED, SKIPPED, STANDARD_ERROR, STANDARD_OUT)
                }
            }
        }
    }
}

dependencies {
    api(projects.commonApi)
    api(projects.sqliteCommonApi)
    api(libs.kermit)
    testImplementation(libs.androidx.test.core)
}

kotlin {
    androidTarget()
    jvm()
    linuxX64()

    sourceSets {
        androidMain.dependencies {
            api(libs.androidx.sqlite.sqlite)
            implementation(libs.androidx.core)
        }
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
        }
    }
}