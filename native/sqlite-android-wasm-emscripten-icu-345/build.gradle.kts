/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("GENERIC_VARIABLE_WRONG_DECLARATION", "UnstableApiUsage")

import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.EMSCRIPTEN_USE_PTHREADS_ATTRIBUTE
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_ATTRIBUTE
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_STATIC
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteCodeGenerationOptions
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteConfigurationOptions
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteExportedFunctions
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.preset.setupAndroidExtensions
import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.preset.setupIcu

/*
 * SQLite WebAssembly Build with Emscripten
 *  * Based on the AOSP SQLite configuration
 *  * Android-specific patches applied
 *  * Android-specific Localized collators
 *  * ICU statically compiled
 *  * No multithreading support
 */
plugins {
    id("ru.pixnews.sqlite-wasm-builder")
    id("ru.pixnews.wasm-sqlite-open-helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm-sqlite-open-helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_wasm_sqlite_android_wasm_emscripten_icu_345_version",
    envVariableName = "WSOH_SQLITE_WASM_SQLITE_ANDROID_WASM_EMSCRIPTEN_ICU_345_VERSION",
).get()

dependencies {
    "wasmLibraries"(projects.native.icuWasm) {
        attributes {
            attribute(EMSCRIPTEN_USE_PTHREADS_ATTRIBUTE, false)
            attribute(ICU_DATA_PACKAGING_ATTRIBUTE, ICU_DATA_PACKAGING_STATIC)
        }
    }
}

sqlite3Build {
    val defaultSqliteVersion = versionCatalogs.named("libs").findVersion("sqlite").get().toString()

    builds {
        create("android-icu") {
            sqliteVersion = defaultSqliteVersion
            codeGenerationOptions = SqliteCodeGenerationOptions.codeGenerationOptions
            emscriptenConfigurationOptions = SqliteCodeGenerationOptions.emscriptenConfigurationOptions
            sqliteConfigOptions = SqliteConfigurationOptions.openHelperConfig(
                enableIcu = true,
                enableMultithreading = false,
            )
            exportedFunctions = SqliteExportedFunctions.openHelperExportedFunctions
            setupIcu(project)
            setupAndroidExtensions(project)
        }
    }
}

val wasmResourcesDir = layout.buildDirectory.dir("wasmLibraries")
val copyResourcesTask = tasks.register<Copy>("copyWasmLibrariesToResources") {
    from(configurations.named("wasmSqliteReleaseElements").get().artifacts.files)

    // Temporary build with unstripped symbols for debugging. Remove in the future or move to a separate
    // build type
    from(configurations.named("wasmSqliteDebugElements").get().artifacts.files)

    into(wasmResourcesDir.map { it.dir("ru/pixnews/wasm/sqlite/open/helper") })
    include("*.wasm")
}

kotlin {
    jvm()
    sourceSets {
        named("jvmMain") {
            resources.srcDir(files(wasmResourcesDir).builtBy(copyResourcesTask))
            dependencies {
                api(projects.sqliteCommonApi)
            }
        }
    }
}
