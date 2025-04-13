/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    kotlin("kapt")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.android")
}

group = "at.released.wasm-sqlite-driver"

android {
    namespace = "at.released.wasm.sqlite.open.helper.test.base"
}

kotlin {
    explicitApi = ExplicitApiMode.Disabled

    sourceSets {
        commonMain.dependencies {
            api(kotlin("test-common"))
            api(kotlin("test-annotations-common"))
            api(projects.sqliteCommon)
            api(libs.wsoh.binary.base)
            api(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kermit)
            implementation(libs.wasi.emscripten.host.test.logger)
        }
        androidMain.dependencies {
            api(libs.androidx.room.runtime26)
            api(libs.androidx.room.testing26)
            compileOnly(kotlin("test-junit5"))
        }
    }
}

dependencies {
    add("annotationProcessor", libs.androidx.room.compiler26)
    add("kapt", libs.androidx.room.compiler26)
}
