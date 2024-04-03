/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage", "GENERIC_VARIABLE_WRONG_DECLARATION")

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.HEADERS_CPLUSPLUS
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.LINK_ARCHIVE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.LINKAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.nativeplatform.MachineArchitecture.ARCHITECTURE_ATTRIBUTE
import org.gradle.nativeplatform.OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.EMSCRIPTEN_USE_PTHREADS_ATTRIBUTE
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_ATTRIBUTE
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_STATIC
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.emscriptenOperatingSystem
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.wasm32Architecture
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

configurations.consumable("wasmIcuElements") {
    attributes {
        attribute(CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(ARCHITECTURE_ATTRIBUTE, objects.wasm32Architecture)
        attribute(OPERATING_SYSTEM_ATTRIBUTE, objects.emscriptenOperatingSystem)
        attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
        attribute(LINKAGE_ATTRIBUTE, Linkage.STATIC)
        attribute(OPTIMIZED_ATTRIBUTE, true)
        attribute(EMSCRIPTEN_USE_PTHREADS_ATTRIBUTE, true)
        attribute(ICU_DATA_PACKAGING_ATTRIBUTE, ICU_DATA_PACKAGING_STATIC)
    }
    outgoing {
        variants {
            create("lib") {
                attributes {
                    attribute(USAGE_ATTRIBUTE, objects.named(Usage.NATIVE_LINK))
                    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LINK_ARCHIVE))
                }
                artifacts {
                    artifact(buildIcuTask.flatMap { it.outputDirectory.dir("lib") }) {
                        builtBy(buildIcuTask)
                    }
                }
            }
            create("include") {
                attributes {
                    attribute(USAGE_ATTRIBUTE, objects.named(Usage.C_PLUS_PLUS_API))
                    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(HEADERS_CPLUSPLUS))
                }
                artifacts {
                    artifact(buildIcuTask.flatMap { it.outputDirectory.dir("include") }) {
                        builtBy(buildIcuTask)
                    }
                }
            }
        }
    }
}
