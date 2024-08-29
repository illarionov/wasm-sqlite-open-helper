/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("CommentWrapping")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module

import at.released.weh.bindings.chicory.ChicoryHostFunctionInstaller
import at.released.weh.bindings.chicory.host.memory.ChicoryMemoryAdapter
import at.released.weh.host.EmbedderHost
import at.released.weh.host.base.WasmModules.ENV_MODULE_NAME
import at.released.weh.host.base.memory.WASM_MEMORY_DEFAULT_MAX_PAGES
import at.released.weh.host.base.memory.WASM_MEMORY_PAGE_SIZE
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
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder.Companion.setupIndirectFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
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
) {
    fun setupModule(): ChicoryInstance {
        val memory = setupMemory(sqlite3Binary.wasmMinMemorySize)

        val memoryAdapter = ChicoryMemoryAdapter(memory.memory())

        val sqliteCallbackFunctionsBuilder = SqliteCallbacksFunctionsBuilder(
            memoryAdapter,
            host,
            callbackStore,
        )

        val installer = ChicoryHostFunctionInstaller(
            memory = memory.memory(),
        ) {
            this.host = this@MainInstanceBuilder.host
        }

        val wasiFunctions = installer.setupWasiPreview1HostFunctions()
        val emscriptenInstaller = installer.setupEmscriptenFunctions()
        val sqliteCallbackFunctions = sqliteCallbackFunctionsBuilder.asChicoryHostFunctions()

        val hostImports = HostImports(
            (emscriptenInstaller.emscriptenFunctions + wasiFunctions + sqliteCallbackFunctions).toTypedArray(),
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
        val emscriptenRuntime = emscriptenInstaller.finalize(instance)

        instance.export(START_FUNCTION_NAME).apply()
        emscriptenRuntime.initMainThread()

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
                WASM_MEMORY_DEFAULT_MAX_PAGES.count.toInt(),
            ),
        ),
    )

    internal class ChicoryInstance(
        val instance: Instance,
        val memory: ChicoryMemoryAdapter,
        val indirectFunctionIndexes: SqliteCallbackFunctionIndexes,
    )
}
