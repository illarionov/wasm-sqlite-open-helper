/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("at.released.wasm.sqlite.open.helper.gradle.documentation.dokka.subproject")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.atomicfu")
    id("at.released.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "at.released.wasm-sqlite-driver"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_common_api_version",
    envVariableName = "WSOH_SQLITE_COMMON_API_VERSION",
).get()

kotlin {
    jvm()
    iosSimulatorArm64()
    iosArm64()
    iosX64()
    linuxArm64()
    linuxX64()
    macosArm64()
    macosX64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io)
            implementation(libs.cassettes.playhead)
            api(libs.wasi.emscripten.host)
            api(libs.wasi.emscripten.host.wasm.core)
            implementation(libs.wasi.emscripten.host.common.util)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
        }
    }
}
