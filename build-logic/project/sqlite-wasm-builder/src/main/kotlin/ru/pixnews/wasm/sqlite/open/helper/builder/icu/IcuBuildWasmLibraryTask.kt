/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.icu

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import ru.pixnews.wasm.sqlite.open.helper.builder.emscripten.EmscriptenSdk
import java.io.File
import javax.inject.Inject

/**
 * Builds the ICU for WASM using Emscripten
 */
@CacheableTask
public abstract class IcuBuildWasmLibraryTask @Inject constructor(
    private val execOperations: ExecOperations,
    objects: ObjectFactory,
    layout: ProjectLayout,
) : DefaultTask() {
    @get:InputDirectory
    @PathSensitive(RELATIVE)
    public val icuSource: DirectoryProperty = objects.directoryProperty()

    @get:Nested
    public val emscriptenSdk: EmscriptenSdk = objects.newInstance()

    @get:InputDirectory
    @PathSensitive(RELATIVE)
    public val icuBuildToolchainDirectory: DirectoryProperty = objects.directoryProperty()

    @get:OutputDirectory
    @Optional
    public val outputDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(ICU_STATIC_LIBRARY_RESULT_DIR),
    )

    @get:Internal
    public val buildDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(ICU_BUILD_DIR),
    )

    @get:Internal
    public val maxJobs: Property<Int> = objects.property<Int>().convention(
        Runtime.getRuntime().availableProcessors(),
    )

    private val icuConfigurePath: File get() = icuSource.file("source/configure").get().asFile

    @TaskAction
    public fun build() {
        emscriptenSdk.checkEmsdkVersion()
        buildWasmIcu()
    }

    private fun buildWasmIcu() {
        val buildDir = buildDirectory.get().asFile
        val dstDir = outputDirectory.get().asFile
        val toolchainDir = icuBuildToolchainDirectory.get().asFile

        val hostEnv = getWasmBuildInstallEnvironment()
        val emConfigure = emscriptenSdk.buildEmconfigureCommandLine {
            add(icuConfigurePath.absolutePath)
        } + listOf(
            "--disable-extras",
            "--disable-icu-config",
            "--disable-samples",
            "--disable-shared",
            "--disable-tests",
            "--enable-static",
            "--enable-tools",
            "--prefix=${dstDir.absolutePath}",
            "--target=wasm32-unknown-emscripten",
            "--with-cross-build=${toolchainDir.absolutePath}",
            "--with-data-packaging=static",
        )
        val emBuild = emscriptenSdk.buildEmMakeCommandLine {
            add("gmake")
            add("-j${maxJobs.get()}")
        }
        val emInstall = emscriptenSdk.buildEmMakeCommandLine {
            add("gmake")
            add("install")
            add("-j${maxJobs.get()}")
        }

        buildDir.deleteRecursively()
        buildDir.mkdirs()

        listOf(
            emConfigure,
            emBuild,
            emInstall,
        ).forEach {
            execOperations.exec {
                commandLine = it
                workingDir = buildDir
                environment = hostEnv
            }.rethrowFailure().assertNormalExitValue()
        }
    }

    private fun getWasmBuildInstallEnvironment(): Map<String, String> {
        return System.getenv().filterKeys {
            it == "PATH"
        } + emscriptenSdk.getEmsdkEnvironment() + mapOf(
            "CXXFLAGS" to ICU_CXXFLAGS.joinToString(" "),
            "CFLAGS" to ICU_CFLAGS.joinToString(" "),
            "ICU_FORCE_LIBS" to ICU_FORCE_LIBS.joinToString(" "),
            "PKGDATA_OPTS" to ICU_PKGDATA_OPTS.joinToString(" "),
            "PATH" to (System.getenv()["PATH"] ?: ""),
        )
    }
}
