/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.icu.internal

import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.ExecOperations
import ru.pixnews.wasm.sqlite.open.helper.builder.emscripten.EmscriptenSdk
import javax.inject.Inject

@CacheableTask
public abstract class IcuBuildTask @Inject constructor(
    private val execOperations: ExecOperations,
    objects: ObjectFactory,
    layout: ProjectLayout,
    providers: ProviderFactory,
) : DefaultTask() {
    @get:InputFiles
    public val icuSource: ConfigurableFileTree = objects.fileTree()

    @get:Nested
    public val emscriptenSdk: EmscriptenSdk = objects.newInstance()

    @get:OutputDirectory
    @Optional
    public val outputDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(ICU_STATIC_LIBRARY_RESULT_DIR),
    )

    @get:Internal
    @Optional
    public val icuBuildToolchainDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(ICU_BUILD_TOOLCHAIN_DIR),
    )

    @get:Internal
    @Optional
    public val icuBuildDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(ICU_BUILD_DIR),
    )

    @TaskAction
    public fun build() {
    }

    internal companion object {
        internal const val ICU_STATIC_LIBRARY_RESULT_DIR = "wasmIcu/out"
        internal const val ICU_BUILD_TOOLCHAIN_DIR = "wasmIcu/buildA"
        internal const val ICU_BUILD_DIR = "wasmIcu/buildB"
    }
}
