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
}

group = "at.released.wasm-sqlite-driver"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_embedder_chicory_version",
    envVariableName = "WSOH_SQLITE_EMBEDDER_CHICORY_VERSION",
).get()

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.sqliteCommon)
            api(libs.cassettes.playhead)
            api(libs.chicory.runtime)
            api(libs.wsoh.binary.base)
            implementation(libs.wasi.emscripten.host.chicory.emscripten)
            implementation(libs.wasi.emscripten.host.chicory.wasip1)
            implementation(libs.wasi.emscripten.host.emscripten.runtime)
            implementation(libs.wsoh.sqlite.st.aot)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.wasi.emscripten.host.fixtures)
            implementation(libs.wasi.emscripten.host.test.logger)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
