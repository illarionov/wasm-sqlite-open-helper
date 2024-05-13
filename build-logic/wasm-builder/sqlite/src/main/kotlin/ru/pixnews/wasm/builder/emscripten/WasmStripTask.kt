/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.emscripten

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class WasmStripTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:InputFile
    public abstract val source: RegularFileProperty

    @get:OutputFile
    public abstract val destination: RegularFileProperty

    @get:Input
    @get:Optional
    public abstract val wasmStripLocation: Property<String>

    @TaskAction
    internal fun strip() {
        val wasmStrip = if (wasmStripLocation.isPresent) {
            wasmStripLocation.get()
        } else {
            "wasm-strip"
        }

        try {
            execOperations.exec {
                commandLine = listOf(
                    wasmStrip,
                    "-o",
                    destination.get().toString(),
                    source.get().toString(),
                )
            }.rethrowFailure()
        } catch (@Suppress("TooGenericExceptionCaught") ioException: Exception) {
            throw GradleException(
                "Failed to execute `wasm-strip`. Make sure WABT (The WebAssembly Binary Toolkit) is installed.",
                ioException,
            )
        }
    }
}
