/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage", "GENERIC_VARIABLE_WRONG_DECLARATION")

package ru.pixnews.wasm.builder.icu

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.HEADERS_CPLUSPLUS
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.LINK_ARCHIVE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.kotlin.dsl.base
import org.gradle.kotlin.dsl.named
import org.gradle.language.cpp.CppBinary.LINKAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.nativeplatform.MachineArchitecture.ARCHITECTURE_ATTRIBUTE
import org.gradle.nativeplatform.OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE
import ru.pixnews.wasm.builder.base.emscripten.EMSCRIPTEN_USE_PTHREADS_ATTRIBUTE
import ru.pixnews.wasm.builder.base.emscripten.emscriptenOperatingSystem
import ru.pixnews.wasm.builder.base.emscripten.wasm32Architecture
import ru.pixnews.wasm.builder.base.ext.capitalizeAscii
import ru.pixnews.wasm.builder.base.ext.firstDirectory
import ru.pixnews.wasm.builder.base.icu.ICU_DATA_PACKAGING_ATTRIBUTE
import ru.pixnews.wasm.builder.icu.IcuBuildDefaults.ICU_BUILD_DIR
import ru.pixnews.wasm.builder.icu.IcuBuildDefaults.ICU_STATIC_LIBRARY_RESULT_DIR
import ru.pixnews.wasm.builder.icu.internal.createIcuSourceConfiguration
import ru.pixnews.wasm.builder.icu.internal.setupUnpackingIcuAttributes

// Convention Plugin for building ICU for WebAssembly using Emscripten
plugins {
    base
}

configurations.consumable("wasmIcuElements") {
    attributes {
        attribute(CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(ARCHITECTURE_ATTRIBUTE, objects.wasm32Architecture)
        attribute(OPERATING_SYSTEM_ATTRIBUTE, objects.emscriptenOperatingSystem)
        attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
        attribute(LINKAGE_ATTRIBUTE, Linkage.STATIC)
        attribute(OPTIMIZED_ATTRIBUTE, true)
    }
}

setupUnpackingIcuAttributes()

internal val icuSources = createIcuSourceConfiguration(
    icuVersion = versionCatalogs.named("libs").findVersion("icu").get().toString(),
).asFileTree

private val icuSourceDir: Provider<File> = icuSources.firstDirectory(providers)

private val icuBuildExtension = extensions.create("icuBuild", IcuWasmBuilderExtension::class.java)

afterEvaluate {
    icuBuildExtension.builds.all {
        setupTasksForBuild(this)
    }
}

private val buildToolchainTask = tasks.register("buildHostIcuToolchain", IcuBuildHostToolchainTask::class.java) {
    group = "Build"
    description = "Compiles ICU Toolchain for local system"

    icuSource.fileProvider(icuSourceDir)
}

private fun setupTasksForBuild(buildSpec: IcuWasmBuildSpec) {
    val buildName = buildSpec.name.capitalizeAscii()
    val outputDirectory = layout.buildDirectory.dir("$ICU_STATIC_LIBRARY_RESULT_DIR-$buildName")
    val buildDirectory = layout.buildDirectory.dir("$ICU_BUILD_DIR-$buildName")

    val buildIcuTask = tasks.register<IcuBuildWasmLibraryTask>("buildIcu$buildName") {
        group = "Build"
        description = "Compiles ICU `$buildName` for WebAssembly"

        icuSource.fileProvider(icuSourceDir)
        emscriptenSdk.emccVersion = versionCatalogs.named("libs").findVersion("emscripten").get().toString()
        icuBuildToolchainDirectory = buildToolchainTask.flatMap(IcuBuildHostToolchainTask::outputDirectory)

        this.outputDirectory = outputDirectory
        this.buildDirectory = buildDirectory
        target = buildSpec.target
        dataPackaging = buildSpec.dataPackaging
        buildFeatures = buildSpec.buildFeatures
        icuAdditionalCflags = buildSpec.icuAdditionalCflags
        icuAdditionalCxxflags = buildSpec.icuAdditionalCxxflags
        icuForceLibs = buildSpec.icuAdditionalForceLibs
        icuUsePthreads = buildSpec.usePthreads
        icuDataDir = buildSpec.icuDataDir
    }

    setupOutgoingArtifacts(buildSpec, buildIcuTask)

    if (buildSpec.buildByDefault.get()) {
        tasks.named("assemble").configure {
            dependsOn(buildIcuTask)
        }
    }
}

private fun setupOutgoingArtifacts(
    buildSpec: IcuWasmBuildSpec,
    buildIcuTask: TaskProvider<IcuBuildWasmLibraryTask>,
) = afterEvaluate {
    configurations["wasmIcuElements"].outgoing.variants {
        val usePthreads = buildSpec.usePthreads.get()
        val dataPackaging = buildSpec.dataPackaging.get()
        val variantSuffix = buildList<String> {
            if (usePthreads) {
                add("pthread")
            }
            add(dataPackaging)
        }
        val libVariantName = (listOf("lib") + variantSuffix).joinToString("-")

        val libVariant = findByName(libVariantName) ?: create(libVariantName) {
            attributes {
                attribute(USAGE_ATTRIBUTE, objects.named(Usage.NATIVE_LINK))
                attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LINK_ARCHIVE))
                attribute(EMSCRIPTEN_USE_PTHREADS_ATTRIBUTE, usePthreads)
                attribute(ICU_DATA_PACKAGING_ATTRIBUTE, dataPackaging)
            }
        }
        libVariant.artifact(buildIcuTask.flatMap { it.outputDirectory.dir("lib") }) {
            builtBy(buildIcuTask)
        }

        val includeVariantName = (listOf("include") + variantSuffix).joinToString("-")
        val includeVariant = findByName(includeVariantName) ?: create(includeVariantName) {
            attributes {
                attribute(USAGE_ATTRIBUTE, objects.named(Usage.C_PLUS_PLUS_API))
                attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(HEADERS_CPLUSPLUS))
                attribute(EMSCRIPTEN_USE_PTHREADS_ATTRIBUTE, usePthreads)
                attribute(ICU_DATA_PACKAGING_ATTRIBUTE, dataPackaging)
            }
        }
        includeVariant.artifact(buildIcuTask.flatMap { it.outputDirectory.dir("include") }) {
            builtBy(buildIcuTask)
        }
    }
}
