/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("GENERIC_VARIABLE_WRONG_DECLARATION", "UnstableApiUsage")

import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteCodeGenerationOptions
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteExportedFunctions

plugins {
    id("ru.pixnews.sqlite-wasm-builder")
    id("ru.pixnews.wasm-sqlite-open-helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm-sqlite-open-helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_wasm_version",
    envVariableName = "WSOH_SQLITE_WASM_VERSION",
).get()

configurations {
    dependencyScope("wasmStaticLibraries")
    resolvable("wasmStaticLibrariesClasspath") {
        extendsFrom(configurations["wasmStaticLibraries"])
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("wasm-library"))
        }
    }
}

dependencies {
    "wasmStaticLibraries"(project(":icu-wasm"))
}

val icuLibrary = configurations["wasmStaticLibrariesClasspath"].asFileTree
val icuLibraryLibs: Provider<File> = icuLibrary.filter { it.name == "libicuuc.a" }
    .elements
    .map { it.first().asFile.parentFile }
val icuLibraryIncludes: Provider<File> = icuLibrary.filter { it.endsWith("include/unicode/ulocale.h") }
    .elements
    .map { it.first().asFile.parentFile.parentFile }

sqlite3Build {
    val defaultSqliteVersion = versionCatalogs.named("libs").findVersion("sqlite").get().toString()
    val sqlite3AndroidSourcesDir = layout.projectDirectory.dir("src/main/cpp/android/android")

    builds {
        create("android-icu-mt-pthread") {
            sqliteVersion = defaultSqliteVersion
            codeGenerationOptions = SqliteCodeGenerationOptions.codeGenerationOptions + listOf(
                "-licuuc",
                "-licui18n",
                "-licudata",
            )
            codeGenerationOptions.add(icuLibraryLibs.map { "-L${it.absolutePath}" })

            additionalSourceFiles.from(
                sqlite3AndroidSourcesDir.files(
                    "sqlite3_android.cpp",
                    "PhoneNumberUtils.cpp",
                    "OldPhoneNumberUtils.cpp",
                ),
            )
            additionalIncludes.from(
                sqlite3AndroidSourcesDir,
                icuLibraryIncludes.map { it.absolutePath },
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
