/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage", "GENERIC_VARIABLE_WRONG_DECLARATION")

import ru.pixnews.wasm.sqlite.open.helper.builder.ext.firstDirectory
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.IcuBuildHostToolchainTask
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.IcuBuildWasmLibraryTask
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.createIcuSourceConfiguration
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.setupUnpackingIcuAttributes

// Convention Plugin for building ICU for WASM using Emscripten
plugins {
    base
}

setupUnpackingIcuAttributes()

internal val icuSources = createIcuSourceConfiguration(
    icuVersion = versionCatalogs.named("libs").findVersion("icu").get().toString(),
).asFileTree

private val icuSourceDir: Provider<File> = icuSources.firstDirectory(providers)

private val buildToolchainTask = tasks.register<IcuBuildHostToolchainTask>("buildHostIcuToolchain") {
    group = "Build"
    description = "Compiles ICU Toolchain for local system"

    icuSource.fileProvider(icuSourceDir)
    emscriptenSdk.emccVersion = versionCatalogs.named("libs").findVersion("emscripten").get().toString()
}

private val buildIcuTask = tasks.register<IcuBuildWasmLibraryTask>("buildIcu") {
    group = "Build"
    description = "Compiles ICU for WASM"

    icuSource.fileProvider(icuSourceDir)
    emscriptenSdk.emccVersion = versionCatalogs.named("libs").findVersion("emscripten").get().toString()
    icuBuildToolchainDirectory = buildToolchainTask.flatMap(IcuBuildHostToolchainTask::outputDirectory)
}

tasks.named("assemble").configure {
    dependsOn(buildIcuTask)
}

private val wasmIcuElements = configurations.consumable("wasmIcuElements") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("wasm-library"))
    }
}

wasmIcuElements.get().outgoing {
    artifacts {
        artifact(buildIcuTask.flatMap(IcuBuildWasmLibraryTask::outputDirectory)) {
            builtBy(buildIcuTask)
        }
    }
}
