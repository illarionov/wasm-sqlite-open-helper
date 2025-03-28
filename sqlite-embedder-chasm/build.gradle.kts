/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("at.released.wasm.sqlite.open.helper.gradle.documentation.dokka.subproject")
    id("at.released.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.publish")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.resources")
}

group = "at.released.wasm-sqlite-driver"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_embedder_chasm_version",
    envVariableName = "WSOH_SQLITE_EMBEDDER_CHASM_VERSION",
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

    sourceSets {
        commonMain.dependencies {
            api(projects.sqliteCommon)
            api(libs.cassettes.playhead)
            api(libs.wsoh.binary.base)
            implementation(libs.chasm)
            compileOnly(libs.chasm.runtime.core)
            implementation(libs.wasi.emscripten.host.chasm.emscripten)
            implementation(libs.wasi.emscripten.host.chasm.wasip1)
            implementation(libs.wasi.emscripten.host.emscripten.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.wasi.emscripten.host.fixtures)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
