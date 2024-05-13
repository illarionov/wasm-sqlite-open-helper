/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite

public object SqliteCodeGenerationOptions {
    @Suppress("ArgumentListWrapping")
    public val codeGenerationOptions: List<String> = listOf(
        "-g3",
        "-fPIC",
        "--minify", "0",
        "--no-entry",
        "-Wno-limited-postlink-optimizations",
    )
    public val codeGenerationOptionsMultithread: List<String> = codeGenerationOptions + listOf(
        "-pthread",
    )
    public val codeOptimizationOptionsO2: List<String> = listOf(
        "-O2",
        "-flto",
    )
    public val emscriptenConfigurationOptions: List<String> = listOf(
        "-sALLOW_MEMORY_GROWTH",
        "-sALLOW_TABLE_GROWTH",
        "-sDYNAMIC_EXECUTION=0",
        "-sENVIRONMENT=worker",
        "-sERROR_ON_UNDEFINED_SYMBOLS",
        "-sEXPORT_ES6",
        "-sEXPORTED_RUNTIME_METHODS=wasmMemory",
        "-sEXPORT_NAME=sqlite3InitModule",
        "-sGLOBAL_BASE=4096",
        "-sIMPORTED_MEMORY",
        "-sINITIAL_MEMORY=16777216",
        "-sLLD_REPORT_UNDEFINED",
        "-sMODULARIZE",
        "-sNO_POLYFILL",
        "-sSTACK_SIZE=512KB",
        "-sSTANDALONE_WASM=0",
        "-sSTRICT_JS=0",
        "-sUSE_CLOSURE_COMPILER=0",
        "-sUSE_ES6_IMPORT_META",
        "-sWASM_BIGINT",
    )
    public val emscriptenConfigurationOptionMultithread: List<String> = emscriptenConfigurationOptions + listOf(
        "-sSHARED_MEMORY",
    )
}
