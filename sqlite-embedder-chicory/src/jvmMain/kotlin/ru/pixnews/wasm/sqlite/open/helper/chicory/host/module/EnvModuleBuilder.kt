/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("CommentWrapping")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module

import com.dylibso.chicory.runtime.HostGlobal
import com.dylibso.chicory.runtime.HostImports
import com.dylibso.chicory.runtime.HostMemory
import com.dylibso.chicory.runtime.HostTable
import com.dylibso.chicory.wasm.types.MemoryLimits
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory.ChicoryMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenEnvFunctionsBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiSnapshotPreview1Builtins
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.WasmSizes
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

internal class EnvModuleBuilder(
    private val host: SqliteEmbedderHost,
    private val moduleName: String = ENV_MODULE_NAME,
    private val minMemorySize: Long = 50_331_648L,
) {
    fun setupModule(): EnvModule {
        val memory = setupMemory(minMemorySize)

        val memoryAdapter = ChicoryMemoryAdapter(memory.memory(), host.rootLogger)

        val wasiFunctions = WasiSnapshotPreview1Builtins(memoryAdapter, host)
            .asChicoryHostFunctions(moduleName)
        val emscriptenFunctions = EmscriptenEnvFunctionsBuilder(memoryAdapter, host)
            .asChicoryHostFunctions(moduleName)

        return EnvModule(
            hostImports = HostImports(
                (emscriptenFunctions + wasiFunctions).toTypedArray(),
                arrayOf<HostGlobal>(),
                memory,
                arrayOf<HostTable>(),
            ),
            memory = memoryAdapter,
            indirectFunctionIndexes = ChicorySqlite3CallbackFunctionIndexes(),
        )
    }

    private fun setupMemory(
        minMemorySize: Long,
    ): HostMemory = HostMemory(
        /* moduleName = */ moduleName,
        /* fieldName = */ "memory",
        /* memory = */
        ChicoryMemory(
            MemoryLimits(
                (minMemorySize / WasmSizes.WASM_MEMORY_PAGE_SIZE).toInt(),
                WasmSizes.WASM_MEMORY_SQLITE_MAX_PAGES.toInt(),
            ),
        ),
    )

    internal class EnvModule(
        val hostImports: HostImports,
        val memory: ChicoryMemoryAdapter,
        val indirectFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    )

    private class ChicorySqlite3CallbackFunctionIndexes(
        override val execCallbackFunction: IndirectFunctionTableIndex = IndirectFunctionTableIndex(0),
        override val traceFunction: IndirectFunctionTableIndex = IndirectFunctionTableIndex(0),
        override val progressFunction: IndirectFunctionTableIndex = IndirectFunctionTableIndex(0),
        override val comparatorFunction: IndirectFunctionTableIndex = IndirectFunctionTableIndex(0),
        override val destroyComparatorFunction: IndirectFunctionTableIndex = IndirectFunctionTableIndex(0),
        override val loggingCallbackFunction: IndirectFunctionTableIndex = IndirectFunctionTableIndex(0),
    ) : Sqlite3CallbackFunctionIndexes
}
