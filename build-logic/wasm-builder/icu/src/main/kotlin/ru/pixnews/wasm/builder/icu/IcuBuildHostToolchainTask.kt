/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.icu

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import ru.pixnews.wasm.builder.icu.IcuBuildDefaults.ICU_BUILD_TOOLCHAIN_DIR
import java.io.File
import javax.inject.Inject

/**
 * Builds the ICU toolchain for the local system, which is then used in cross-compilation
 * to generate and package data.
 *
 * https://unicode-org.github.io/icu/userguide/icu4c/build.html#how-to-cross-compile-icu
 */
@CacheableTask
public abstract class IcuBuildHostToolchainTask @Inject constructor(
    private val execOperations: ExecOperations,
    objects: ObjectFactory,
    layout: ProjectLayout,
) : DefaultTask() {
    @get:InputDirectory
    @PathSensitive(RELATIVE)
    public val icuSource: DirectoryProperty = objects.directoryProperty()

    @get:OutputDirectory
    @Optional
    public val outputDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(ICU_BUILD_TOOLCHAIN_DIR),
    )

    @get:Internal
    public val maxJobs: Property<Int> = objects.property<Int>().convention(
        Runtime.getRuntime().availableProcessors(),
    )

    private val icuConfigurePath: File get() = icuSource.file("source/configure").get().asFile

    @TaskAction
    public fun build() {
        buildToolchain()
    }

    private fun buildToolchain() {
        val dstDir = outputDirectory.get().asFile
        dstDir.mkdirs()

        val hostEnv = System.getenv().filterKeys {
            it == "PATH"
        }

        execOperations.exec {
            commandLine = listOf(icuConfigurePath.absolutePath)
            workingDir = dstDir
            environment = hostEnv
        }.rethrowFailure().assertNormalExitValue()

        execOperations.exec {
            commandLine = listOf("gmake", "-j${maxJobs.get()}")
            workingDir = dstDir
            environment = hostEnv
        }.rethrowFailure().assertNormalExitValue()
    }
}
