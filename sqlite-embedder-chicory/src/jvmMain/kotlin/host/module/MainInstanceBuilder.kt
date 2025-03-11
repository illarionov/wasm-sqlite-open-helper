/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("CommentWrapping")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module

import at.released.cassettes.base.AssetUrl
import at.released.cassettes.playhead.AssetManager
import at.released.cassettes.playhead.readOrThrow
import at.released.weh.bindings.chicory.ChicoryEmscriptenHostInstaller
import at.released.weh.bindings.chicory.memory.ChicoryMemoryAdapter
import at.released.weh.bindings.chicory.memory.ChicoryMemoryProvider
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.WasmModules.ENV_MODULE_NAME
import at.released.weh.wasm.core.memory.WASM_MEMORY_DEFAULT_MAX_PAGES
import at.released.weh.wasm.core.memory.WASM_MEMORY_PAGE_SIZE
import com.dylibso.chicory.runtime.ByteBufferMemory
import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.ImportMemory
import com.dylibso.chicory.runtime.ImportValues
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Instance.START_FUNCTION_NAME
import com.dylibso.chicory.runtime.Machine
import com.dylibso.chicory.wasm.Parser
import com.dylibso.chicory.wasm.WasmModule
import com.dylibso.chicory.wasm.types.MemoryLimits
import kotlinx.io.asInputStream
import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.SqliteCallbacksFunctionsBuilder.Companion.setupIndirectFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import java.io.InputStream

internal class MainInstanceBuilder(
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
    private val sqlite3Binary: WasmSqliteConfiguration,
    private val wasmSourceReader: AssetManager,
    private val machineFactory: ((Instance) -> Machine)?,
) {
    fun setupModule(): ChicoryInstance {
        val memoryLimits = MemoryLimits(
            (sqlite3Binary.wasmMinMemorySize / WASM_MEMORY_PAGE_SIZE).toInt(),
            WASM_MEMORY_DEFAULT_MAX_PAGES.count.toInt(),
        )
        // XXX: should not be used directly
        val chicoryMemory = ByteBufferMemory(memoryLimits)

        val memoryAdapter = ChicoryMemoryAdapter(chicoryMemory)

        val sqliteCallbackFunctionsBuilder = SqliteCallbacksFunctionsBuilder(
            memoryAdapter,
            host,
            callbackStore,
        )

        val installer = ChicoryEmscriptenHostInstaller {
            this.host = this@MainInstanceBuilder.host
            this.memoryProvider = ChicoryMemoryProvider { memoryAdapter }
        }

        val wasiFunctions: List<HostFunction> = installer.setupWasiPreview1HostFunctions()
        val emscriptenInstaller = installer.setupEmscriptenFunctions()
        val sqliteCallbackFunctions = sqliteCallbackFunctionsBuilder.asChicoryHostFunctions()

        val hostImports = ImportValues.builder()
            .withFunctions(emscriptenInstaller.emscriptenFunctions + wasiFunctions + sqliteCallbackFunctions)
            .addMemory(ImportMemory(ENV_MODULE_NAME, "memory", chicoryMemory))
            .build()

        val sqlite3Module: WasmModule =
            wasmSourceReader.readOrThrow(AssetUrl(sqlite3Binary.sqliteUrl.url)) { source, _ ->
                runCatching {
                    source.buffered().asInputStream().use { sourceStream: InputStream ->
                        Parser.parse(sourceStream)
                    }
                }
            }

        val instance = Instance
            .builder(sqlite3Module)
            .withImportValues(hostImports)
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

    internal class ChicoryInstance(
        val instance: Instance,
        val memory: ChicoryMemoryAdapter,
        val indirectFunctionIndexes: SqliteCallbackFunctionIndexes,
    )
}
