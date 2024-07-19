import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.google.devtools.ksp")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
}

group = "ru.pixnews.wasm-sqlite-open-helper"

kotlin {
    explicitApi = ExplicitApiMode.Disabled

    iosSimulatorArm64()
    iosArm64()
    iosX64()
    jvm()
    linuxX64()
    macosArm64()
    macosX64()

    sourceSets {
        commonMain.dependencies {
            api(kotlin("test-common"))
            api(kotlin("test-annotations-common"))
            api(libs.androidx.sqlite.sqlite)
            api(projects.sqliteCommon)
            api(libs.wsoh.binary.base)
            api(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.testing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
        }
        jvmMain.dependencies {
            compileOnly(kotlin("test-junit"))
        }
    }
}

dependencies {
    ksp(libs.androidx.room.compiler)
}
