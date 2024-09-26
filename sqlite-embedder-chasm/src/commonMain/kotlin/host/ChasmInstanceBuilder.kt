/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host

import at.released.weh.bindings.chasm.ChasmHostFunctionInstaller
import at.released.weh.bindings.chasm.memory.ChasmMemoryAdapter
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.WasmModules.ENV_MODULE_NAME
import at.released.weh.wasm.core.memory.WASM_MEMORY_DEFAULT_MAX_PAGES
import at.released.weh.wasm.core.memory.WASM_MEMORY_PAGE_SIZE
import com.github.michaelbull.result.getOrThrow
import io.github.charlietap.chasm.embedding.error.ChasmError.DecodeError
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.memory
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.Function
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Limits
import io.github.charlietap.chasm.embedding.shapes.Module
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.executor.runtime.ext.asRange
import io.github.charlietap.chasm.executor.runtime.ext.table
import io.github.charlietap.chasm.executor.runtime.instance.TableInstance
import io.github.charlietap.chasm.executor.runtime.store.Address
import io.github.charlietap.chasm.executor.runtime.value.ReferenceValue
import io.github.charlietap.chasm.stream.SourceReader
import kotlinx.io.RawSource
import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.binary.reader.readOrThrow
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.orThrow
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.exception.ChasmErrorException
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.exception.ChasmException
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.ChasmSqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.setupSqliteCallbacksHostFunctions
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import io.github.charlietap.chasm.embedding.shapes.Import as ChasmImport
import io.github.charlietap.chasm.embedding.shapes.Memory as ChasmMemoryImportable
import io.github.charlietap.chasm.embedding.shapes.MemoryType as ChasmMemoryType

internal class ChasmInstanceBuilder(
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
    private val sqlite3Binary: WasmSqliteConfiguration,
    private val sourceReader: WasmSourceReader,
) {
    fun setupChasmInstance(): ChasmInstance {
        val store: Store = store()
        val memoryImport = setupMemory(store, sqlite3Binary.wasmMinMemorySize)
        val memoryProvider: Store.() -> ChasmMemoryImportable = { memoryImport.memory }
        val memoryAdapter = ChasmMemoryAdapter(
            store = store,
            memoryProvider = memoryProvider,
        )

        val chasmInstaller = ChasmHostFunctionInstaller(store) {
            this.host = this@ChasmInstanceBuilder.host
            this.memoryProvider = memoryProvider
        }
        val wasiHostFunctions = chasmInstaller.setupWasiPreview1HostFunctions()
        val emscriptenInstaller = chasmInstaller.setupEmscriptenFunctions()
        val sqliteCallbacksHostFunctions = setupSqliteCallbacksHostFunctions(store, memoryAdapter, host, callbackStore)

        val hostImports: List<Import> = buildList {
            add(memoryImport.import)
            addAll(emscriptenInstaller.emscriptenFunctions)
            addAll(wasiHostFunctions)
            addAll(sqliteCallbacksHostFunctions)
        }

        val sqliteModule: Module = sourceReader.readOrThrow(sqlite3Binary.sqliteUrl) { source: RawSource, _ ->
            val sourceReader: SourceReader = source.buffered().toChasmSourceReader()
            module(sourceReader = sourceReader)
                .fold(
                    onSuccess = { Result.success(it) },
                    onError = { error: DecodeError ->
                        Result.failure(ChasmErrorException(error, "Can not decode $sqlite3Binary; $error"))
                    },
                )
        }

        val instance: Instance = instance(
            store = store,
            module = sqliteModule,
            imports = hostImports,
        ).orThrow { "Can not instantiate $sqlite3Binary" }

        val indirectFunctionTableIndexes = setupIndirectFunctionIndexes(store, instance, sqliteCallbacksHostFunctions)

        val emscriptenRuntime = emscriptenInstaller.finalize(instance)
        emscriptenRuntime.initMainThread()

        return ChasmInstance(store, instance, memoryAdapter, indirectFunctionTableIndexes)
    }

    private fun setupMemory(
        store: Store,
        minMemorySize: Long,
    ): ChasmMemoryImport {
        val memoryType = ChasmMemoryType(
            Limits(
                min = (minMemorySize / WASM_MEMORY_PAGE_SIZE).toUInt(),
                max = WASM_MEMORY_DEFAULT_MAX_PAGES.count.toUInt(),
            ),
        )
        val memory: ChasmMemoryImportable = memory(store, memoryType)
        val import = ChasmImport(ENV_MODULE_NAME, "memory", memory)
        return ChasmMemoryImport(memoryType, memory, import)
    }

    @Suppress("INVISIBLE_MEMBER")
    private fun setupIndirectFunctionIndexes(
        store: Store,
        instance: Instance,
        callbackHostFunctions: List<ChasmImport>,
    ): SqliteCallbackFunctionIndexes {
        val tableAddress = instance.instance.tableAddresses[0]
        val table = store.store.table(tableAddress).getOrThrow {
            ChasmException("Can not get table $tableAddress")
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
                val functionAddress = (import.value as Function).reference.address
                table.elements[indirectIndex] = ReferenceValue.Function(functionAddress)
                hostFunction to IndirectFunctionTableIndex(indirectIndex)
            }.toMap()

        return ChasmSqliteCallbackFunctionIndexes(indirectIndexes)
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

    private class ChasmMemoryImport(
        val type: ChasmMemoryType,
        val memory: ChasmMemoryImportable,
        val import: ChasmImport,
    )

    internal class ChasmInstance(
        val store: Store,
        val instance: Instance,
        val memory: ChasmMemoryAdapter,
        val indirectFunctionIndexes: SqliteCallbackFunctionIndexes,
    )
}
