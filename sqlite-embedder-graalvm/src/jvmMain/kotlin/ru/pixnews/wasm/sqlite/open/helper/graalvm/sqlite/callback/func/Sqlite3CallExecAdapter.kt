/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore.Sqlite3ExecCallbackId
import ru.pixnews.wasm.sqlite.open.helper.host.memory.readPtr

internal const val SQLITE3_EXEC_CB_FUNCTION_NAME = "sqlite3_exec_cb"

internal class Sqlite3CallExecAdapter(
    language: WasmLanguage,
    module: WasmModule,
    private val callbackStore: Sqlite3CallbackStore,
    host: SqliteEmbedderHost,
    functionName: String,
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args = frame.arguments
        return callDelegate(
            memory(frame),
            args.getArgAsInt(0),
            args.getArgAsInt(1),
            args.getArgAsWasmPtr(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @CompilerDirectives.TruffleBoundary
    @Suppress("VARIABLE_HAS_PREFIX")
    private fun callDelegate(
        memory: WasmMemory,
        arg1: Int,
        columns: Int,
        pResults: WasmPtr<WasmPtr<Byte>>,
        pColumnNames: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        logger.v { "Calling exec callback arg1: $arg1 columns: $columns names: $pColumnNames results: $pResults" }
        val hostMemory = memory.toHostMemory()
        val delegateId = Sqlite3ExecCallbackId(arg1)
        val delegate = callbackStore.sqlite3ExecCallbacks[delegateId] ?: error("Callback $delegateId not registered")

        val columnNames = (0 until columns).map { columnNo ->
            val ptr: WasmPtr<Byte> = hostMemory.readPtr(pColumnNames + (columnNo * WASM_SIZEOF_PTR.toInt()))
            hostMemory.readNullTerminatedString(ptr)
        }

        val results = (0 until columns).map { columnNo ->
            val ptr: WasmPtr<Byte> = hostMemory.readPtr(pResults + (columnNo * WASM_SIZEOF_PTR.toInt()))
            hostMemory.readNullTerminatedString(ptr)
        }
        return delegate(columnNames, results)
    }
}
