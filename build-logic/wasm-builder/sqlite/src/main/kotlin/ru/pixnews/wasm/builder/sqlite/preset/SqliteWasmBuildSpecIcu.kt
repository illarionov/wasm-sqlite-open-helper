/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite.preset

import org.gradle.api.Project
import org.gradle.api.file.FileSystemLocation
import org.gradle.kotlin.dsl.get
import ru.pixnews.wasm.builder.sqlite.SqliteWasmBuildSpec
import java.io.File

public fun SqliteWasmBuildSpec.setupIcu(
    project: Project,
    initialMemory: Int = 50_331_648,
) {
    additionalLibs.from(
        project.configurations["wasmStaticLibrariesClasspath"].elements.map { locations: Set<FileSystemLocation> ->
            val libsDir = locations.first().asFile
            listOf("icuuc", "icui18n", "icudata").map { File(libsDir, "$it.a") }
        },
    )
    additionalIncludes.from(
        project.configurations["wasmHeadersClasspath"].elements.map { locations: Set<FileSystemLocation> ->
            locations.map { it.asFile.absolutePath }
        },
    )
    emscriptenConfigurationOptions.set(
        emscriptenConfigurationOptions.get()
            .filter { !it.startsWith("-sINITIAL_MEMORY=") }
            .toList() + "-sINITIAL_MEMORY=$initialMemory",
    )
}
