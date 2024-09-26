/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.publish")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.resources")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
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
    mingwX64 {
        binaries.all {
            linkerOpts("-lole32")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sqliteCommon)
            api(libs.wsoh.binary.reader)
            implementation(libs.chasm.runtime)
            implementation(libs.wasi.emscripten.host.chasm)
            implementation(libs.wasi.emscripten.host.emscripten.runtime)
            implementation(libs.wsoh.sqlite.st)
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
