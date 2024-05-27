/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite.internal

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class UnpackSqliteAmalgamationTransform @Inject constructor(
    private val archiveOperations: ArchiveOperations,
    private val fileSystemOperations: FileSystemOperations,
) : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputZipFile: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val zipFile = inputZipFile.get().asFile
        val unzipDir = outputs.dir(zipFile.nameWithoutExtension).toPath()

        val fileTree = archiveOperations.zipTree(zipFile)

        fileSystemOperations.sync {
            from(fileTree)
            into(unzipDir)
            include { it.name in SQLITE_EXTRACTED_FILES }
            eachFile { path = sourceName }
        }
    }

    internal companion object {
        val SQLITE_EXTRACTED_FILES = setOf("sqlite3.c", "sqlite3.h", "shell.c")
    }
}
