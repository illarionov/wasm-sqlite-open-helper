/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.wasm.builder.sqlite.internal

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import ru.pixnews.sqlite.open.helper.wasm.builder.sqlite.SqliteCodeGenerationOptions.codeGenerationOptions
import ru.pixnews.sqlite.open.helper.wasm.builder.sqlite.SqliteCodeGenerationOptions.codeOptimizationOptionsO2
import ru.pixnews.sqlite.open.helper.wasm.builder.sqlite.SqliteCodeGenerationOptions.emscriptenConfigurationOptions
import ru.pixnews.sqlite.open.helper.wasm.builder.sqlite.SqliteConfigurationOptions
import ru.pixnews.sqlite.open.helper.wasm.builder.sqlite.SqliteExportedFunctions
import java.io.File

internal class SqliteAdditionalArgumentProvider(
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val sqliteCFile: Provider<File>,
) : CommandLineArgumentProvider {
    override fun asArguments(): MutableIterable<String> {
        return mutableListOf<String>().apply {
            addAll(codeGenerationOptions)
            addAll(codeOptimizationOptionsO2)
            addAll(emscriptenConfigurationOptions)
            addAll(exportedFunctionsConfiguration)
            addAll(SqliteConfigurationOptions.wasmConfig)
            add("""-DSQLITE_WASM_EXPORT=""") // we specify all exported functions in -sEXPORTED_FUNCTIONS
            add("-DSQLITE_C=${sqliteCFile.get()}")
        }
    }

    companion object {
        val exportedFunctionsConfiguration = listOf(
            // "-sEXPORTED_FUNCTIONS=${ExportedFunctions.defaultWasm.joinToString(",")}",
            "-sEXPORTED_FUNCTIONS=${SqliteExportedFunctions.openHelper.joinToString(",")}",
        )
    }
}
