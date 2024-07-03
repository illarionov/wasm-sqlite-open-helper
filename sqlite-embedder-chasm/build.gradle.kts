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
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.chasm)
            implementation(libs.okio.okio)
            compileOnly(libs.chasm.decoder)
            compileOnly(libs.chasm.decoder.wasm)
            compileOnly(libs.chasm.instantiator)
            compileOnly(libs.chasm.memory)
            compileOnly(libs.chasm.runtime)
            compileOnly(libs.chasm.validator)
            api(projects.sqliteCommon)
            implementation(projects.wasiEmscriptenHost)
            implementation(libs.wsoh.sqlite.st)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
