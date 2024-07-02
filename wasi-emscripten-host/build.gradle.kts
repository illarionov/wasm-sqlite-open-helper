/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_wasi_emscripten_host_version",
    envVariableName = "WSOH_WASI_EMSCRIPTEN_HOST_VERSION",
).get()

kotlin {
    jvm()
    js {
        nodejs()
    }
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            api(projects.commonApi)
            implementation(libs.okio.okio)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(kotlin("test"))
            implementation(libs.assertk)
        }
    }
}
