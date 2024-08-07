/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host

import com.github.michaelbull.result.getOrThrow
import io.github.charlietap.chasm.ast.module.Module
import io.github.charlietap.chasm.ast.type.Limits
import io.github.charlietap.chasm.ast.type.MemoryType
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.memory
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.error.ChasmError.DecodeError
import io.github.charlietap.chasm.executor.runtime.ext.asRange
import io.github.charlietap.chasm.executor.runtime.ext.table
import io.github.charlietap.chasm.executor.runtime.ext.tableAddress
import io.github.charlietap.chasm.executor.runtime.instance.ExternalValue
import io.github.charlietap.chasm.executor.runtime.instance.ExternalValue.Memory
import io.github.charlietap.chasm.executor.runtime.instance.ModuleInstance
import io.github.charlietap.chasm.executor.runtime.instance.TableInstance
import io.github.charlietap.chasm.executor.runtime.store.Address
import io.github.charlietap.chasm.executor.runtime.store.Store
import io.github.charlietap.chasm.executor.runtime.value.ReferenceValue
import io.github.charlietap.chasm.fold
import kotlinx.io.RawSource
import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.binary.reader.readOrThrow
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.orThrow
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.exception.ChasmErrorException
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.exception.ChasmException
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.exception.ChasmModuleRuntimeErrorException
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory.ChasmMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.getEmscriptenHostFunctions
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.ChasmSqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.getSqliteCallbacksHostFunctions
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.getWasiPreview1HostFunctions
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_PAGE_SIZE
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_SQLITE_MAX_PAGES
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import io.github.charlietap.chasm.import.Import as ChasmImport

internal class ChasmInstanceBuilder(
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
    private val emscriptenStackRef: () -> EmscriptenStack,
    private val sqlite3Binary: WasmSqliteConfiguration,
    private val sourceReader: WasmSourceReader,
) {
    fun setupChasmInstance(): ChasmInstance {
        val store: Store = store()

        val memoryImport = setupMemory(store, sqlite3Binary.wasmMinMemorySize)
        val memory = ChasmMemoryAdapter(store, (memoryImport.value as Memory).address)
        val wasiMemoryReader = DefaultWasiMemoryReader(memory, host.fileSystem, host.rootLogger)
        val wasiMemoryWriter = DefaultWasiMemoryWriter(memory, host.fileSystem, host.rootLogger)

        val sqliteCallbacksHostFunctions = getSqliteCallbacksHostFunctions(store, memory, host, callbackStore)

        val hostImports = buildList {
            add(memoryImport)
            addAll(getEmscriptenHostFunctions(store, memory, host, emscriptenStackRef))
            addAll(getWasiPreview1HostFunctions(store, memory, wasiMemoryReader, wasiMemoryWriter, host))
            addAll(sqliteCallbacksHostFunctions)
        }

        val sqliteModule: Module = sourceReader.readOrThrow(sqlite3Binary.sqliteUrl) { source: RawSource, _ ->
            module(source.buffered().toChasmSourceReader())
                .fold(
                    onSuccess = { Result.success(it) },
                    onError = { error: DecodeError ->
                        Result.failure(ChasmErrorException(error, "Can not decode $sqlite3Binary; $error"))
                    },
                )
        }

        val instance: ModuleInstance = instance(store, sqliteModule, hostImports)
            .orThrow { "Can not instantiate $sqlite3Binary" }

        val indirectFunctionTableIndexes = setupIndirectFunctionIndexes(store, instance, sqliteCallbacksHostFunctions)

        return ChasmInstance(store, instance, memory, indirectFunctionTableIndexes)
    }

    private fun setupMemory(
        store: Store,
        minMemorySize: Long,
    ): ChasmImport {
        val memory: Memory = memory(
            store,
            MemoryType(
                Limits(
                    min = (minMemorySize / WASM_MEMORY_PAGE_SIZE).toUInt(),
                    max = WASM_MEMORY_SQLITE_MAX_PAGES.count.toUInt(),
                ),
            ),
        )

        return ChasmImport(ENV_MODULE_NAME, "memory", memory)
    }

    private fun setupIndirectFunctionIndexes(
        store: Store,
        instance: ModuleInstance,
        callbackHostFunctions: List<ChasmImport>,
    ): Sqlite3CallbackFunctionIndexes {
        val tableAddress = instance.tableAddress(0)
        val table: TableInstance = store.table(tableAddress.value).getOrThrow {
            ChasmModuleRuntimeErrorException(it)
        }
        val oldSize = table.elements.size
        table.grow(
            callbackHostFunctions.size,
            ReferenceValue.Function(Address.Function(0)),
        )
        val indirectIndexes: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex> =
            callbackHostFunctions.mapIndexed { index: Int, import: ChasmImport ->
                val hostFunction = SqliteCallbacksModuleFunction.byWasmName.getValue(import.entityName)
                val indirectIndex = oldSize + index
                val functionAddress = (import.value as ExternalValue.Function).address
                table.elements[indirectIndex] = ReferenceValue.Function(functionAddress)
                hostFunction to IndirectFunctionTableIndex(indirectIndex)
            }.toMap()

        return ChasmSqlite3CallbackFunctionIndexes(indirectIndexes)
    }

    private fun TableInstance.grow(
        elementsToAdd: Int,
        referenceValue: ReferenceValue,
    ) {
        val proposedLength = (elements.size + elementsToAdd).toUInt()
        if (proposedLength !in type.limits.asRange()) {
            throw ChasmException("Failed to grow table to $proposedLength")
        }
        type = type.copy(
            limits = type.limits.copy(
                min = proposedLength,
            ),
        )
        elements += Array(elementsToAdd) { referenceValue }
    }

    internal class ChasmInstance(
        val store: Store,
        val instance: ModuleInstance,
        val memory: ChasmMemoryAdapter,
        val indirectFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    )
}
