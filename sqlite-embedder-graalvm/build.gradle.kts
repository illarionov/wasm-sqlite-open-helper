/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.sqlite.open.helper.gradle.graalvm")
}

group = "ru.pixnews.sqlite.open.helper.graalvm"

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            api(projects.sqliteCommonApi)
            implementation(projects.wasiEmscriptenHost)
            //implementation(project(":wasi-emscripten-host"))
            api(libs.graalvm.polyglot.polyglot)
            implementation(libs.graalvm.polyglot.wasm)
            compileOnly(libs.graalvm.wasm.language)
        }
    }
}
