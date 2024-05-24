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
    propertiesFileKey = "wsoh_sqlite_embedder_chicory_version",
    envVariableName = "WSOH_SQLITE_EMBEDDER_CHICORY_VERSION",
).get()

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.sqliteCommon)
            implementation(libs.chicory)
            implementation(projects.wasiEmscriptenHost)
            implementation(projects.native.sqliteAndroidWasmEmscriptenIcu346)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
