/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite.internal

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

internal class SqliteAdditionalArgumentProvider(
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val sqliteCFile: Provider<File>,
    @get:Input
    val codeGenerationOptions: Provider<List<String>>,
    @get:Input
    val codeOptimizationOptions: Provider<List<String>>,
    @get:Input
    val emscriptenConfigurationOptions: Provider<List<String>>,
    @get:Input
    val exportedFunctions: Provider<List<String>>,
    @get:Input
    val sqliteConfigOptions: Provider<List<String>>,
    ) : CommandLineArgumentProvider {
    override fun asArguments(): MutableIterable<String> {
        return mutableListOf<String>().apply {
            addAll(codeGenerationOptions.get())
            addAll(codeOptimizationOptions.get())
            addAll(emscriptenConfigurationOptions.get())
            add("-sEXPORTED_FUNCTIONS=${exportedFunctions.get().joinToString(",")}")
            addAll(sqliteConfigOptions.get())
            add("""-DSQLITE_WASM_EXPORT=""") // we specify all exported functions in -sEXPORTED_FUNCTIONS
            add("-DSQLITE_C=${sqliteCFile.get()}")
        }
    }
}
