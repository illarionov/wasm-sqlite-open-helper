/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.base.emscripten

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.ExecOperations
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

public abstract class EmscriptenSdk @Inject constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    private val execOperations: ExecOperations,
) {
    @get:Input
    @Optional
    public val emscriptenRoot: Property<File> = objects.property(File::class.java).convention(
        providers
            .environmentVariable("EMSDK")
            .orElse(providers.gradleProperty("emsdkRoot"))
            .map(::File),
    )

    @get:Input
    @get:Optional
    public val emccVersion: Property<String> = objects.property(String::class.java)
        .convention("3.1.55")

    @get:Internal
    public val emscriptenCacheDir: DirectoryProperty = objects.directoryProperty()

    @get:InputFiles
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public val emscriptenConfigFile: ConfigurableFileCollection = objects.fileCollection()

    @get:Internal
    public val emccExecutablePath: Property<String> = objects.property(String::class.java)
        .convention("upstream/emscripten/emcc")

    @get:Internal
    public val emConfigureExecutablePath: Property<String> = objects.property(String::class.java)
        .convention("upstream/emscripten/emconfigure")

    @get:Internal
    public val emMakeExecutablePath: Property<String> = objects.property(String::class.java)
        .convention("upstream/emscripten/emmake")

    public fun buildEmccCommandLine(
        builderAction: MutableList<String>.() -> Unit,
    ): List<String> = buildCommandLine(emccExecutablePath, builderAction)

    public fun buildEmconfigureCommandLine(
        builderAction: MutableList<String>.() -> Unit,
    ): List<String> = buildCommandLine(emConfigureExecutablePath, builderAction)

    public fun buildEmMakeCommandLine(
        builderAction: MutableList<String>.() -> Unit,
    ): List<String> = buildCommandLine(emMakeExecutablePath, builderAction)

    private fun buildCommandLine(
        commandExecutablePath: Property<String>,
        builderAction: MutableList<String>.() -> Unit,
    ): List<String> = buildList {
        val command = getEmscriptenExecutableOrThrow(commandExecutablePath)
        add(command.toString())
        // Do not depend on ~/.emscripten
        add("--em-config")
        add(getEmscriptenConfigFile().toString())

        if (emscriptenCacheDir.isPresent) {
            val cacheDir = emscriptenCacheDir.get()
            add("--cache")
            add(cacheDir.toString())
        }

        builderAction()
    }

    public fun checkEmsdkVersion() {
        if (!emccVersion.isPresent) {
            return
        }
        val requiredVersion = emccVersion.get()
        val version = readEmsdkVersion()

        if (requiredVersion != version) {
            throw IllegalStateException(
                "The installed version of Emscripten SDK `$version` differs from the required" +
                        " version `$requiredVersion`",
            )
        }
    }

    @Internal
    public fun getEmsdkEnvironment(): Map<String, String> = buildMap {
        put("EMSDK", emscriptenRoot.get().toString())
    }

    private fun readEmsdkVersion(): String {
        val emcc = getEmscriptenExecutableOrThrow(emccExecutablePath).toString()

        val stdErr = ByteArrayOutputStream()
        execOperations.exec {
            commandLine = listOf(emcc, "-v")
            errorOutput = stdErr
            environment = getEmsdkEnvironment()
        }.rethrowFailure().assertNormalExitValue()

        val firstLine: String = ByteArrayInputStream(stdErr.toByteArray()).bufferedReader().use {
            it.readLine()
        } ?: error("Can not read Emscripten SDK version")

        return EMCC_VERSION_REGEX.matchEntire(firstLine)?.groups?.get(1)?.value
            ?: error("Can not parse EMSDK version from `$firstLine`. ")
    }

    private fun getEmscriptenExecutableOrThrow(
        commandPath: Provider<String>,
    ): File {
        val pathProvider = emscriptenRoot.zip(commandPath, ::File)
        val path = pathProvider.orNull ?: error(
            "Can not find Emscripten SDK installation directory. EMSDK environment variable should be defined",
        )
        check(path.isFile) {
            "Can not find Emscripten executable. `$path` is not a file"
        }
        return path
    }

    private fun getEmscriptenConfigFile(): File {
        val files = emscriptenConfigFile.files
        return if (files.isNotEmpty()) {
            files.first()
        } else {
            emscriptenRoot.get().resolve(".emscripten")
        }
    }

    private companion object {
        private val EMCC_VERSION_REGEX = """emcc\s+\(Emscripten.+\)\s+(\S+)\s+.*""".toRegex()
    }
}
