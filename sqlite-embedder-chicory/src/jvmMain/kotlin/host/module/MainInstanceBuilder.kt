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
import com.dylibso.chicory.runtime.Machine
import com.dylibso.chicory.runtime.Module
import com.dylibso.chicory.runtime.Module.START_FUNCTION_NAME
import com.dylibso.chicory.wasm.types.MemoryLimits
import kotlinx.io.asInputStream
import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.binary.reader.readOrThrow
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory.ChicoryMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory.ChicoryWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory.ChicoryWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenEnvFunctionsBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder.Companion.setupIndirectFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiSnapshotPreview1ModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_PAGE_SIZE
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_SQLITE_MAX_PAGES
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import java.io.InputStream
import com.dylibso.chicory.log.Logger as ChicoryLogger
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

@Suppress("COMMENTED_OUT_CODE")
internal class MainInstanceBuilder(
    private val host: EmbedderHost,
    private val chicoryLogger: ChicoryLogger,
    private val callbackStore: SqliteCallbackStore,
    private val sqlite3Binary: WasmSqliteConfiguration,
    private val wasmSourceReader: WasmSourceReader,
    private val machineFactory: ((Instance) -> Machine)?,
    private val stackBindingsRef: () -> EmscriptenStack,
) {
    fun setupModule(): ChicoryInstance {
        val memory = setupMemory(sqlite3Binary.wasmMinMemorySize)

        val memoryAdapter = ChicoryMemoryAdapter(memory.memory())
        val wasiMemoryReader: WasiMemoryReader = ChicoryWasiMemoryReader.createOrDefault(
            memoryAdapter,
            host.fileSystem,
            host.rootLogger,
        )
        val wasiMemoryWriter: WasiMemoryWriter = ChicoryWasiMemoryWriter.createOrDefault(
            memoryAdapter,
            host.fileSystem,
            host.rootLogger,
        )
        val sqliteCallbackFunctionsBuilder = SqliteCallbacksFunctionsBuilder(
            memoryAdapter,
            host,
            callbackStore,
        )

        val wasiFunctions = WasiSnapshotPreview1ModuleBuilder(memoryAdapter, wasiMemoryReader, wasiMemoryWriter, host)
            .asChicoryHostFunctions()
        val emscriptenFunctions = EmscriptenEnvFunctionsBuilder(memoryAdapter, host, stackBindingsRef)
            .asChicoryHostFunctions()
        val sqliteCallbackFunctions = sqliteCallbackFunctionsBuilder.asChicoryHostFunctions()

        val hostImports = HostImports(
            (emscriptenFunctions + wasiFunctions + sqliteCallbackFunctions).toTypedArray(),
            arrayOf<HostGlobal>(),
            memory,
            arrayOf<HostTable>(),
        )

        val sqlite3Module: Module = wasmSourceReader.readOrThrow(sqlite3Binary.sqliteUrl) { source, _ ->
            runCatching {
                source.buffered().asInputStream().use { sourceStream: InputStream ->
                    Module
                        .builder(sourceStream)
                        .withLogger(chicoryLogger)
                        .withHostImports(hostImports)
                        .withInitialize(true)
                        .withStart(false)
                        .run {
                            if (machineFactory != null) {
                                withMachineFactory(machineFactory)
                            } else {
                                this
                            }
                        }
                        .build()
                }
            }
        }

        val instance = sqlite3Module.instantiate()
        val indirectFunctionTableIndexes = setupIndirectFunctionIndexes(instance)
        instance.export(START_FUNCTION_NAME).apply()

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
                (minMemorySize / WASM_MEMORY_PAGE_SIZE).toInt(),
                WASM_MEMORY_SQLITE_MAX_PAGES.count.toInt(),
            ),
        ),
    )

    internal class ChicoryInstance(
        val instance: Instance,
        val memory: ChicoryMemoryAdapter,
        val indirectFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    )
}
