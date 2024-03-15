/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")

plugins {
    id("ru.pixnews.sqlite.open.helper.wasm.builder")
    id("ru.pixnews.sqlite.open.helper.gradle.multiplatform.kotlin")
}

val defaultSqliteVersion = versionCatalogs.named("libs").findVersion("sqlite").get().toString()

group = "ru.pixnews.sqlite.open.helper.sqlite.wasm"

sqlite3Build {
    builds {
        create("main") {
            sqliteVersion = defaultSqliteVersion
        }
    }
}

val wasmResourcesDir = layout.buildDirectory.dir("wasmLibraries")
val copyResourcesTask = tasks.register<Copy>("copyWasmLibrariesToResources") {
    from(configurations.named("wasmSqliteElements").get().artifacts.files)
    into(wasmResourcesDir.map { it.dir("ru/pixnews/sqlite/open/helper/sqlite/wasm") })
    include("*.wasm")
}

kotlin {
    jvm()
    sourceSets {
        named("jvmMain") {
            resources.srcDir(files(wasmResourcesDir).builtBy(copyResourcesTask))
        }
    }
}
