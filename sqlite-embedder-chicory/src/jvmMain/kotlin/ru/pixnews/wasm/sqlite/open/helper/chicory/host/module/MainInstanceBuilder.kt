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
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Module
import com.dylibso.chicory.wasm.types.MemoryLimits
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory.ChicoryMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenEnvFunctionsBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder.Companion.setupIndirectFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiSnapshotPreview1Builtins
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmSizes
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

@Suppress("COMMENTED_OUT_CODE")
internal class MainInstanceBuilder(
    private val host: SqliteEmbedderHost,
    private val callbackStore: SqliteCallbackStore,
    private val minMemorySize: Long = 50_331_648L,
) {
    fun setupModule(
        sqlite3Binary: WasmSqliteConfiguration,
    ): ChicoryInstance {
        val memory = setupMemory(minMemorySize)

        val memoryAdapter = ChicoryMemoryAdapter(memory.memory(), host.rootLogger)
        val sqliteCallbackFunctionsBuilder = SqliteCallbacksFunctionsBuilder(
            memoryAdapter,
            host,
            callbackStore,
        )

        val wasiFunctions = WasiSnapshotPreview1Builtins(memoryAdapter, host)
            .asChicoryHostFunctions()
        val emscriptenFunctions = EmscriptenEnvFunctionsBuilder(memoryAdapter, host)
            .asChicoryHostFunctions()
        val sqliteCallbackFunctions = sqliteCallbackFunctionsBuilder.asChicoryHostFunctions()

        val hostImports = HostImports(
            (emscriptenFunctions + wasiFunctions + sqliteCallbackFunctions).toTypedArray(),
            arrayOf<HostGlobal>(),
            memory,
            arrayOf<HostTable>(),
        )

        val sqlite3Module: Module = sqlite3Binary.sqliteUrl.openStream().use {
            Module.builder(it).build()
        }

        val instance = sqlite3Module.instantiate(hostImports)
        val indirectFunctionTableIndexes = setupIndirectFunctionIndexes(instance)
// New version (Choicory 0.0.9+):
//        val instance = sqlite3Module.instantiate(hostImports, true, false)
//        val indirectFunctionTableIndexes = setupIndirectFunctionIndexes(instance)
//        instance.export(START_FUNCTION_NAME).apply()

        return ChicoryInstance(
            instance = instance,
            memory = memoryAdapter,
            indirectFunctionIndexes = indirectFunctionTableIndexes,
        )
    }

    private fun setupMemory(
        minMemorySize: Long,
    ): HostMemory = HostMemory(
        /* moduleName = */ ENV_MODULE_NAME,
        /* fieldName = */ "memory",
        /* memory = */
        ChicoryMemory(
            MemoryLimits(
                (minMemorySize / WasmSizes.WASM_MEMORY_PAGE_SIZE).toInt(),
                WasmSizes.WASM_MEMORY_SQLITE_MAX_PAGES.toInt(),
            ),
        ),
    )

    internal class ChicoryInstance(
        val instance: Instance,
        val memory: ChicoryMemoryAdapter,
        val indirectFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    )
}
