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
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import ru.pixnews.wasm.builder.sqlite.internal.ApplyAndroidPatchesSqliteTransform.Parameters
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class ApplyAndroidPatchesSqliteTransform @Inject constructor(
    private val execOperations: ExecOperations,
) : TransformAction<Parameters> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputDir: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputDir = inputDir.get().asFile
        val dstDirName = inputDir.name + "-patched"

        val outputDir: File = outputs.dir(dstDirName)
        inputDir.copyRecursively(outputDir)

        val androidPatchFile = parameters.androidSqlitePatchFile.get().asFile
        execOperations.exec {
            this.commandLine("patch", "--strip=0", "--silent")
            this.setStandardInput(androidPatchFile.inputStream())
            this.workingDir = outputDir
        }
        outputDir.walkBottomUp().forEach { file ->
            if (file.extension == "orig") {
                file.delete()
            }
        }
    }

    interface Parameters : TransformParameters {
        @get:InputFile
        var androidSqlitePatchFile: Provider<RegularFile>
    }
}
