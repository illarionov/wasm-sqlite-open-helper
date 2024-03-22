/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")

import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteCodeGenerationOptions
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteConfigurationOptions


plugins {
    id("ru.pixnews.sqlite-wasm-builder")
    id("ru.pixnews.wasm-sqlite-open-helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm-sqlite-open-helper.gradle.multiplatform.publish")
}

val defaultSqliteVersion = versionCatalogs.named("libs").findVersion("sqlite").get().toString()

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_wasm_version",
    envVariableName = "WSOH_SQLITE_WASM_VERSION",
).get()

val androidSaliteSpecifics = listOf(
    "-DSQLITE_DEFAULT_AUTOVACUUM=1",
    "-DSQLITE_DEFAULT_FILE_FORMAT=4",
    "-DSQLITE_DEFAULT_FILE_PERMISSIONS=0600",
    "-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576",
    "-DSQLITE_ENABLE_BATCH_ATOMIC_WRITE",
    "-DSQLITE_ENABLE_MEMORY_MANAGEMENT=1",
)

sqlite3Build {
    builds {
        create("main") {
            sqliteVersion = defaultSqliteVersion
        }
        create("main-mt") {
            sqliteVersion = defaultSqliteVersion
            sqliteConfigOptions = SqliteConfigurationOptions.wasmConfig -
                    listOf("-DSQLITE_THREADSAFE=0") +
                    listOf("-DSQLITE_THREADSAFE=2")
        }
        create("main-mt-pthread") {
            sqliteVersion = defaultSqliteVersion
            codeGenerationOptions = SqliteCodeGenerationOptions.codeGenerationOptions + listOf(
                "-pthread",
            )
            sqliteConfigOptions = SqliteConfigurationOptions.wasmConfig -
                    listOf("-DSQLITE_THREADSAFE=0") +
                    listOf("-DSQLITE_THREADSAFE=2")
        }
    }
}

val wasmResourcesDir = layout.buildDirectory.dir("wasmLibraries")
val copyResourcesTask = tasks.register<Copy>("copyWasmLibrariesToResources") {
    from(configurations.named("wasmSqliteElements").get().artifacts.files)
    into(wasmResourcesDir.map { it.dir("ru/pixnews/wasm/sqlite/open/helper") })
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
