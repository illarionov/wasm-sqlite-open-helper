/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.graalvm")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.publish")
    kotlin("kapt")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_embedder_graalvm_version",
    envVariableName = "WSOH_SQLITE_EMBEDDER_GRAALVM_VERSION",
).get()

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sqliteCommon)
            implementation(libs.wsoh.sqlite.mt)
            implementation(projects.wasiEmscriptenHost)
        }
        jvmMain.dependencies {
            api(libs.graalvm.polyglot.polyglot)
            compileOnly(libs.graalvm.wasm.language)
            implementation(libs.graalvm.polyglot.wasm)
            implementation(libs.graalvm.truffle.api)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    add("kapt", libs.graalvm.truffle.dsl.processor)
}
