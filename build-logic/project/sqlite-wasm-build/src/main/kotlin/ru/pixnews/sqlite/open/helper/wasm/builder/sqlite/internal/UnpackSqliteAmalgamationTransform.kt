/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.wasm.builder.sqlite.internal

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.name

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class UnpackSqliteAmalgamationTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputZipFile: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val zipFile = inputZipFile.get().asFile
        val unzipDir = outputs.dir(zipFile.nameWithoutExtension).toPath()

        ZipFile(zipFile).use { inputZip: ZipFile -> unpackRequiredEntries(inputZip, unzipDir) }
    }

    private fun unpackRequiredEntries(zipFile: ZipFile, dstDir: Path) {
        for (entry: ZipEntry in zipFile.entries()) {
            if (entry.isDirectory) {
                continue
            }
            val fname = Path(entry.name).name
            if (fname in SQLITE_EXTRACTED_FILES) {
                zipFile.getInputStream(entry).use {
                    Files.copy(it, dstDir.resolve(fname))
                }
            }
        }
    }

    internal companion object {
        val SQLITE_EXTRACTED_FILES = listOf("sqlite3.c", "sqlite3.h")
    }
}
