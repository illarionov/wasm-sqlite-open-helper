/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")

import ru.pixnews.wasm.sqlite.open.helper.builder.icu.internal.IcuBuildTask
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteCodeGenerationOptions
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteExportedFunctions

plugins {
    id("ru.pixnews.icu-wasm-builder")
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

private val buildIcuTask = tasks.named<IcuBuildTask>("buildIcu")

sqlite3Build {
    builds {
        create("android-icu-mt-pthread") {
            sqliteVersion = defaultSqliteVersion
            val sqlite3AndroidSourcesDir = layout.projectDirectory.dir("src/main/cpp/android/android")
            codeGenerationOptions = SqliteCodeGenerationOptions.codeGenerationOptions + listOf(
                "-licuuc",
                "-licui18n",
                "-licudata",
            )
            codeOptimizationOptions.add(
                buildIcuTask.flatMap { it.outputDirectory.dir("lib") }.map { "-L${it.asFile.absolutePath}" },
            )

            additionalSourceFiles.from(
                sqlite3AndroidSourcesDir.files(
                    "sqlite3_android.cpp",
                    "PhoneNumberUtils.cpp",
                    "OldPhoneNumberUtils.cpp",
                ),
            )
            additionalIncludes.from(
                sqlite3AndroidSourcesDir,
                buildIcuTask.flatMap { it.outputDirectory.dir("include") }.map { it.asFile.absolutePath },
            )
            emscriptenConfigurationOptions = SqliteCodeGenerationOptions.emscriptenConfigurationOptions -
                    "-sINITIAL_MEMORY=16777216" +
                    "-sINITIAL_MEMORY=50331648"
            exportedFunctions = SqliteExportedFunctions.openHelperExportedFunctions + listOf(
                "_register_localized_collators",
                "_register_android_functions",
            )
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
