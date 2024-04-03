/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage", "GENERIC_VARIABLE_WRONG_DECLARATION")

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.HEADERS_CPLUSPLUS
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.LINK_ARCHIVE
import org.gradle.api.attributes.Usage.C_PLUS_PLUS_API
import org.gradle.api.attributes.Usage.NATIVE_LINK
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.DEBUGGABLE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.LINKAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.nativeplatform.Linkage.STATIC
import org.gradle.nativeplatform.MachineArchitecture.ARCHITECTURE_ATTRIBUTE
import org.gradle.nativeplatform.OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.emscriptenOperatingSystem
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.wasm32Architecture
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.wasmBinaryLibraryElements
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.wasmRuntimeUsage
import ru.pixnews.wasm.sqlite.open.helper.builder.emscripten.EmscriptenBuildTask
import ru.pixnews.wasm.sqlite.open.helper.builder.emscripten.WasmStripTask
import ru.pixnews.wasm.sqlite.open.helper.builder.ext.capitalizeAscii
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteWasmBuildSpec
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteWasmBuilderExtension
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.BuildDirPath.STRIPPED_RESULT_DIR
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.BuildDirPath.compileUnstrippedResultDir
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.SqliteAdditionalArgumentProvider
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.createSqliteSourceConfiguration
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal.setupUnpackingSqliteAttributes

// Convention Plugin for building Sqlite WASM using Emscripten
plugins {
    base
}

setupUnpackingSqliteAttributes(
    androidSqlitePatchFile = project.layout.projectDirectory.file(provider { "src/main/cpp/android/Android.patch" }),
)

configurations {
    dependencyScope("wasmLibraries")

    consumable("wasmSqliteReleaseElements") {
        attributes {
            attribute(USAGE_ATTRIBUTE, objects.wasmRuntimeUsage)
            attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
            attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.wasmBinaryLibraryElements)

            attribute(ARCHITECTURE_ATTRIBUTE, objects.wasm32Architecture)
            attribute(OPERATING_SYSTEM_ATTRIBUTE, objects.emscriptenOperatingSystem)

            attribute(DEBUGGABLE_ATTRIBUTE, false)
            attribute(OPTIMIZED_ATTRIBUTE, true)
        }
    }
    consumable("wasmSqliteDebugElements") {
        attributes {
            attribute(USAGE_ATTRIBUTE, objects.wasmRuntimeUsage)
            attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
            attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.wasmBinaryLibraryElements)

            attribute(ARCHITECTURE_ATTRIBUTE, objects.wasm32Architecture)
            attribute(OPERATING_SYSTEM_ATTRIBUTE, objects.emscriptenOperatingSystem)

            attribute(OPTIMIZED_ATTRIBUTE, true)
            attribute(DEBUGGABLE_ATTRIBUTE, true)
        }
    }
    resolvable("wasmStaticLibrariesClasspath") {
        description = "Static libraries from included libraries used to link Sqlite"
        extendsFrom(configurations["wasmLibraries"])
        attributes {
            attribute(USAGE_ATTRIBUTE, objects.named(NATIVE_LINK))
            attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
            attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LINK_ARCHIVE))
            attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
            attribute(LINKAGE_ATTRIBUTE, STATIC)

            attribute(ARCHITECTURE_ATTRIBUTE, objects.wasm32Architecture)
            attribute(OPERATING_SYSTEM_ATTRIBUTE, objects.emscriptenOperatingSystem)
        }
    }
    resolvable("wasmHeadersClasspath") {
        description = "Header files from included WASM libraries used to compile Sqlite"
        extendsFrom(configurations["wasmLibraries"])
        attributes {
            attribute(USAGE_ATTRIBUTE, objects.named(C_PLUS_PLUS_API))
            attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
            attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(HEADERS_CPLUSPLUS))
            attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
            attribute(LINKAGE_ATTRIBUTE, STATIC)

            attribute(OPERATING_SYSTEM_ATTRIBUTE, objects.emscriptenOperatingSystem)
            attribute(ARCHITECTURE_ATTRIBUTE, objects.wasm32Architecture)
        }
    }
}

private val sqliteExtension = extensions.create("sqlite3Build", SqliteWasmBuilderExtension::class.java)

afterEvaluate {
    sqliteExtension.builds.configureEach {
        setupTasksForBuild(this)
    }
}

private fun setupTasksForBuild(buildSpec: SqliteWasmBuildSpec) {
    val buildName = buildSpec.name.capitalizeAscii()
    val sqlite3c: FileCollection = if (buildSpec.sqlite3Source.isEmpty) {
        createSqliteSourceConfiguration(buildSpec.sqliteVersion)
    } else {
        buildSpec.sqlite3Source
    }
    val unstrippedWasmFileName = buildSpec.wasmUnstrippedFileName.get()
    val unstrippedJsFileName = unstrippedWasmFileName.substringBeforeLast(".wasm") + ".mjs"
    val strippedWasmFileName = buildSpec.wasmFileName.get()

    val compileSqliteTask = tasks.register<EmscriptenBuildTask>("compileSqlite$buildName") {
        val sqlite3cFile = sqlite3c.elements.map { it.first().asFile }

        group = "Build"
        description = "Compiles SQLite `$buildName` to Wasm"
        sourceFiles.from(buildSpec.additionalSourceFiles)
        outputFileName = unstrippedJsFileName
        outputDirectory = layout.buildDirectory.dir(compileUnstrippedResultDir(buildName))
        emscriptenSdk.emccVersion = versionCatalogs.named("libs").findVersion("emscripten").get().toString()
        includes.setFrom(
            sqlite3cFile.map { it.parentFile },
            buildSpec.additionalIncludes,
        )

        val additionalArgsProvider = SqliteAdditionalArgumentProvider(
            sqlite3cFile,
            codeGenerationOptions = buildSpec.codeGenerationOptions,
            codeOptimizationOptions = buildSpec.codeOptimizationOptions,
            emscriptenConfigurationOptions = buildSpec.emscriptenConfigurationOptions,
            exportedFunctions = buildSpec.exportedFunctions,
            sqliteConfigOptions = buildSpec.sqliteConfigOptions,
        )
        additionalArgumentProviders.add(additionalArgsProvider)
    }

    val stripSqliteTask: TaskProvider<WasmStripTask> = tasks.register<WasmStripTask>("stripSqlite$buildName") {
        group = "Build"
        description = "Strip compiled SQLite `$buildName` Wasm binary"
        source = compileSqliteTask.flatMap { it.outputDirectory.file(unstrippedWasmFileName) }
        val dstDir = layout.buildDirectory.dir(STRIPPED_RESULT_DIR)
        destination = dstDir.map { it.file(strippedWasmFileName) }
        doFirst {
            dstDir.get().asFile.let { dir ->
                dir.walkBottomUp()
                    .filter { it != dir }
                    .forEach(File::delete)
            }
        }
    }

    configurations.named("wasmSqliteReleaseElements").get().outgoing {
        artifacts {
            artifact(stripSqliteTask.flatMap(WasmStripTask::destination)) {
                builtBy(stripSqliteTask)
            }
        }
    }
    configurations.named("wasmSqliteDebugElements").get().outgoing {
        artifacts {
            artifact(compileSqliteTask.flatMap { it.outputDirectory.file(unstrippedWasmFileName) }) {
                builtBy(compileSqliteTask)
            }
        }
    }
    tasks.named("assemble").configure {
        dependsOn(stripSqliteTask)
    }
}
