/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chasm.host

import at.released.cassettes.base.AssetUrl
import at.released.cassettes.playhead.AssetManager
import at.released.cassettes.playhead.readOrThrow
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.open.helper.chasm.ext.orThrow
import at.released.wasm.sqlite.open.helper.chasm.host.exception.ChasmErrorException
import at.released.wasm.sqlite.open.helper.chasm.host.exception.ChasmException
import at.released.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.ChasmSqliteCallbackFunctionIndexes
import at.released.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.setupSqliteCallbacksHostFunctions
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import at.released.weh.bindings.chasm.ChasmEmscriptenHostBuilder
import at.released.weh.bindings.chasm.memory.ChasmMemoryAdapter
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.WasmModules.ENV_MODULE_NAME
import at.released.weh.wasm.core.memory.WASM_MEMORY_DEFAULT_MAX_PAGES
import at.released.weh.wasm.core.memory.WASM_MEMORY_PAGE_SIZE
import io.github.charlietap.chasm.config.RuntimeConfig
import io.github.charlietap.chasm.embedding.error.ChasmError.DecodeError
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.memory
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.shapes.Function
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Module
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.runtime.address.Address
import io.github.charlietap.chasm.runtime.ext.asRange
import io.github.charlietap.chasm.runtime.ext.table
import io.github.charlietap.chasm.runtime.ext.toLong
import io.github.charlietap.chasm.runtime.instance.FunctionInstance
import io.github.charlietap.chasm.runtime.instance.TableInstance
import io.github.charlietap.chasm.runtime.value.ReferenceValue
import io.github.charlietap.chasm.stream.SourceReader
import io.github.charlietap.chasm.type.Limits
import io.github.charlietap.chasm.type.SharedStatus.Unshared
import kotlinx.io.RawSource
import kotlinx.io.buffered
import io.github.charlietap.chasm.embedding.shapes.Import as ChasmImport
import io.github.charlietap.chasm.embedding.shapes.Memory as ChasmMemoryImportable
import io.github.charlietap.chasm.type.MemoryType as ChasmMemoryType

internal class ChasmInstanceBuilder(
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
    private val sqlite3Binary: WasmSqliteConfiguration,
    private val sourceReader: AssetManager,
    private val runtimeConfig: RuntimeConfig,
) {
    fun setupChasmInstance(): ChasmInstance {
        val store: Store = store()
        val memoryImport = setupMemory(store, sqlite3Binary.wasmMinMemorySize)
        val memoryProvider: Store.() -> ChasmMemoryImportable = { memoryImport.memory }
        val memoryAdapter = ChasmMemoryAdapter(
            store = store,
            memoryProvider = memoryProvider,
        )

        val chasmInstaller = ChasmEmscriptenHostBuilder(store) {
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

        val sqliteModule: Module =
            sourceReader.readOrThrow(AssetUrl(sqlite3Binary.sqliteUrl.url)) { source: RawSource, _ ->
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
            config = runtimeConfig,
        ).orThrow { "Can not instantiate $sqlite3Binary" }

        val indirectFunctionTableIndexes = setupIndirectFunctionIndexes(store, instance, sqliteCallbacksHostFunctions)

        fixFunctionRtts(store, instance, sqliteCallbacksHostFunctions)

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
            shared = Unshared,
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
        val table: TableInstance = store.store.table(instance.instance.tableAddresses[0])
        val oldSize = table.elements.size
        table.grow(callbackHostFunctions.size, ReferenceValue.Function(Address.Function(0)))
        val indirectIndexes = callbackHostFunctions.mapIndexed { index: Int, import: ChasmImport ->
            val hostFunction: SqliteCallbacksModuleFunction =
                SqliteCallbacksModuleFunction.byWasmName.getValue(import.entityName)
            val indirectIndex = oldSize + index
            val functionAddress = (import.value as Function).reference.address
            table.elements[indirectIndex] = ReferenceValue.Function(functionAddress).toLong()
            hostFunction to IndirectFunctionTableIndex(indirectIndex)
        }.toMap()

        return ChasmSqliteCallbackFunctionIndexes(indirectIndexes)
    }

    // Updates FunctionInstance.HostFunction.rtt to fix checks of the callback function types.
    // Required on Chasm 0.9.65
    @Suppress("INVISIBLE_MEMBER")
    private fun fixFunctionRtts(
        store: Store,
        instance: Instance,
        callbackHostFunctions: List<ChasmImport>,
    ) {
        val storeFunctions: MutableList<FunctionInstance> = store.store.functions
        val rtts = instance.instance.runtimeTypes
        callbackHostFunctions.forEach { import: ChasmImport ->
            val functionAddress = (import.value as Function).reference.address.address
            val f: FunctionInstance.HostFunction = storeFunctions[functionAddress] as FunctionInstance.HostFunction
            storeFunctions[functionAddress] = f.copy(
                rtt = rtts.first { it.type == f.type },
            )
        }
    }

    private fun TableInstance.grow(
        elementsToAdd: Int,
        referenceValue: ReferenceValue.Function,
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
        elements += LongArray(elementsToAdd) { referenceValue.toLong() }
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
