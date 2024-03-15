/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.sqlite.open.helper.gradle.graalvm")
    id("ru.pixnews.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.sqlite.open.helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_embedder_graalvm_version",
    envVariableName = "WSOH_SQLITE_EMBEDDER_GRAALVM_VERSION",
).get()

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            api(projects.sqliteCommonApi)
            implementation(project(":wasi-emscripten-host"))
            api(libs.graalvm.polyglot.polyglot)
            implementation(libs.graalvm.polyglot.wasm)
            compileOnly(libs.graalvm.wasm.language)
        }
    }
}
