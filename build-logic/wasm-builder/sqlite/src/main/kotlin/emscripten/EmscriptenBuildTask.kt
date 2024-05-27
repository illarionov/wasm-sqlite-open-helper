/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.emscripten

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import ru.pixnews.wasm.builder.base.emscripten.EmscriptenSdk
import ru.pixnews.wasm.builder.sqlite.internal.BuildDirPath
import java.io.File
import javax.inject.Inject

@CacheableTask
public abstract class EmscriptenBuildTask @Inject constructor(
    private val execOperations: ExecOperations,
    objects: ObjectFactory,
    layout: ProjectLayout,
) : DefaultTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val sourceFiles: ConfigurableFileCollection = objects.fileCollection()

    @get:Nested
    public val emscriptenSdk: EmscriptenSdk = objects.newInstance()

    @get:Input
    public val outputFileName: Property<String> = objects.property(String::class.java)

    @get:OutputDirectory
    @Optional
    public val outputDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(BuildDirPath.compileUnstrippedResultDir("Main")),
    )

    @get:Input
    @Optional
    public val workingDir: Property<File> = objects.property(File::class.java).convention(
        layout.buildDirectory.dir(BuildDirPath.COMPILE_WORK_DIR).map { it.asFile },
    )

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    public val includes: ConfigurableFileCollection = objects.fileCollection()

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    public val libs: ConfigurableFileCollection = objects.fileCollection()

    @get:Internal
    public val outputFile: RegularFileProperty = objects.fileProperty().convention(
        outputDirectory.zip(outputFileName, Directory::file),
    )

    @get:Nested
    public val additionalArgumentProviders: ListProperty<CommandLineArgumentProvider> = objects.listProperty()

    @TaskAction
    public fun build() {
        emscriptenSdk.checkEmsdkVersion()

        val workingDir = this@EmscriptenBuildTask.workingDir.get()
        workingDir.mkdirs()

        val cmdLine = buildCommandLine()

        execOperations.exec {
            this.commandLine = cmdLine
            this.workingDir = workingDir
            this.environment = emscriptenSdk.getEmsdkEnvironment()
        }.rethrowFailure().assertNormalExitValue()
    }

    private fun buildCommandLine(): List<String> = emscriptenSdk.buildEmccCommandLine {
        val workDir = this@EmscriptenBuildTask.workingDir.get()

        add("-o")
        add(outputFile.get().toString())

        includes.forEach { includePath ->
            val relativePath = includePath.relativeToOrSelf(workDir)
            add("-I$relativePath")
        }

        addAll(getLinkerCommandLineArguments())

        additionalArgumentProviders.get().forEach { argumentProvider ->
            addAll(argumentProvider.asArguments())
        }

        sourceFiles.forEach { sourcePath ->
            val relativePath = sourcePath.relativeToOrSelf(workDir)
            add(relativePath.toString())
        }
    }

    private fun getLinkerCommandLineArguments(): List<String> {
        val libs = libs.files

        val libraryNames = libs.map {
            it.name.substringBeforeLast(".a")
        }
        require(libraryNames.toSet().size == libs.size) {
            "Library names not unique: `$libs`"
        }

        val libraryPaths = libs.mapTo(mutableSetOf()) {
            it.parentFile.absolutePath
        }

        return buildList {
            libraryNames.mapTo(this) { "-l$it" }
            libraryPaths.mapTo(this) { "-L$it" }
        }
    }
}
