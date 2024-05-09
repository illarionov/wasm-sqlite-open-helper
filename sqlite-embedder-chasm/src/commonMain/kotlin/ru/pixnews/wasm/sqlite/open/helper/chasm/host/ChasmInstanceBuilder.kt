/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host

import io.github.charlietap.chasm.ast.type.Limits
import io.github.charlietap.chasm.ast.type.MemoryType
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.memory
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.executor.runtime.instance.ExternalValue.Memory
import io.github.charlietap.chasm.executor.runtime.instance.ModuleInstance
import io.github.charlietap.chasm.executor.runtime.store.Store
import io.github.charlietap.chasm.import.Import
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.orThrow
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory.ChasmMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.getEmscriptenHostFunctions
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.ChasmSqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.getSqliteCallbacksHostFunctions
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.getWasiPreview1HostFunctions
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmSizes
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmSizes.WASM_MEMORY_PAGE_SIZE

internal class ChasmInstanceBuilder(
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
    private val minMemorySize: Long = 50_331_648L,
) {
    fun setupChasmInstance(
        sqlite3Binary: WasmSqliteConfiguration,
    ): ChasmInstance {
        val store: Store = store()

        val memoryImport = setupMemory(store, minMemorySize)
        val memory = ChasmMemoryAdapter(
            store,
            (memoryImport.value as Memory).address,
            host.fileSystem,
            host.rootLogger,
        )

        val hostImports = buildList {
            add(memoryImport)
            addAll(getEmscriptenHostFunctions(store, memory, host))
            addAll(getWasiPreview1HostFunctions(store, memory, host))
            addAll(getSqliteCallbacksHostFunctions(store, memory, host, callbackStore))
        }

        val sqliteBinaryBytes = sqlite3Binary.sqliteUrl.readBytes()
        val sqliteModule = module(sqliteBinaryBytes).orThrow { "Can not decode $sqlite3Binary" }

        val instance: ModuleInstance = instance(store, sqliteModule, hostImports)
            .orThrow { "Can not instantiate $sqlite3Binary" }

        val indirectFunctionTableIndexes = setupIndirectFunctionIndexes(instance)

        return ChasmInstance(store, instance, memory, indirectFunctionTableIndexes)
    }

    private fun setupMemory(
        store: Store,
        minMemorySize: Long,
    ): Import {
        val memory: Memory = memory(
            store,
            MemoryType(
                Limits(
                    min = (minMemorySize / WASM_MEMORY_PAGE_SIZE).toUInt(),
                    max = WasmSizes.WASM_MEMORY_SQLITE_MAX_PAGES.toUInt(),
                ),
            ),
        )

        return Import(ENV_MODULE_NAME, "memory", memory)
    }

    private fun setupIndirectFunctionIndexes(
        @Suppress("UnusedParameter") instance: ModuleInstance,
    ): Sqlite3CallbackFunctionIndexes {
        // TODO
        val indirectIndexes: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex> = emptyMap()
        return ChasmSqlite3CallbackFunctionIndexes(indirectIndexes)
    }

    internal class ChasmInstance(
        val store: Store,
        val instance: ModuleInstance,
        val memory: ChasmMemoryAdapter,
        val indirectFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    )
}
