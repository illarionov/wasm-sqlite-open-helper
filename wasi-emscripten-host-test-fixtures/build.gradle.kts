/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
}

group = "ru.pixnews.wasm-sqlite-open-helper"

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }
    linuxX64()
    macosArm64()
    macosX64()

    sourceSets {
        commonMain.dependencies {
            api(projects.wasiEmscriptenHost)
            api(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.okio.okio)
            implementation(libs.kotlinx.datetime)
        }
    }
}
