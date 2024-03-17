/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage", "GENERIC_VARIABLE_WRONG_DECLARATION")

import ru.pixnews.wasm.sqlite.open.helper.builder.emscripten.EmscriptenBuildTask
import ru.pixnews.wasm.sqlite.open.helper.builder.emscripten.WasmStripTask
import ru.pixnews.wasm.sqlite.open.helper.builder.ext.capitalizeAscii
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteWasmBuildSpec
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteWasmBuilderExtension
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.BuildDirPath.STRIPPED_RESULT_DIR
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.BuildDirPath.compileUnstrippedResultDir
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.SqliteAdditionalArgumentProvider
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.createSqliteSourceConfiguration
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.setupUnpackSqliteAttributes

// Convention Plugin for building Sqlite WASM using Emscripten
plugins {
    base
}

setupUnpackSqliteAttributes()

configurations {
    dependencyScope("wasmLibraries")

    consumable("wasmSqliteElements") {
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("wasm-library"))
        }
    }
    resolvable("wasmLibrariesClasspath") {
        extendsFrom(configurations["wasmLibraries"])
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("wasm-library"))
        }
    }
}

private val sqliteExtension = extensions.create("sqlite3Build", SqliteWasmBuilderExtension::class.java)

sqliteExtension.builds.configureEach {
    setupTasksForBuild(this)
}

private fun setupTasksForBuild(buildSpec: SqliteWasmBuildSpec) {
    val buildName = buildSpec.name.capitalizeAscii()
    val sqliteWasmFilesSrdDir = layout.projectDirectory.dir("src/main/cpp/sqlite")
    val sqlite3c: FileCollection = if (buildSpec.sqlite3Source.isEmpty) {
        createSqliteSourceConfiguration(buildSpec.sqliteVersion.get())
    } else {
        buildSpec.sqlite3Source
    }
    val unstrippedWasmFileName = buildSpec.wasmUnstrippedFileName.get()
    val unstrippedJsFileName = unstrippedWasmFileName.substringBeforeLast(".wasm") + ".js"
    val strippedWasm = buildSpec.wasmFileName.get()

    val compileSqliteTask = tasks.register<EmscriptenBuildTask>("compileSqlite$buildName") {
        val sqlite3cFile = sqlite3c.elements.map { it.first().asFile }

        group = "Build"
        description = "Compiles SQLite `$buildName` to Wasm"
        source.from(sqliteWasmFilesSrdDir.file("wasm/api/sqlite3-wasm.c"))
        outputFileName = unstrippedJsFileName
        outputDirectory = layout.buildDirectory.dir(compileUnstrippedResultDir(buildName))
        emccVersion = versionCatalogs.named("libs").findVersion("emscripten").get().toString()
        includes.setFrom(
            sqlite3cFile.map { it.parentFile },
            sqliteWasmFilesSrdDir.dir("wasm/api"),
        )
        additionalArgumentProviders.add(SqliteAdditionalArgumentProvider(sqlite3cFile))
    }

    val stripSqliteTask: TaskProvider<WasmStripTask> = tasks.register<WasmStripTask>("stripSqlite$buildName") {
        group = "Build"
        description = "Strips compiled SQLite `$buildName` Wasm binary"
        source.set(compileSqliteTask.flatMap { it.outputDirectory.file(unstrippedWasmFileName) })
        destination.set(layout.buildDirectory.dir(STRIPPED_RESULT_DIR).map { it.file(strippedWasm) })
    }

    configurations.named("wasmSqliteElements").get().outgoing {
        artifacts {
            artifact(stripSqliteTask.flatMap(WasmStripTask::destination)) {
                builtBy(stripSqliteTask)
            }
        }
    }
    tasks.named("assemble").configure {
        dependsOn(stripSqliteTask)
    }
}
