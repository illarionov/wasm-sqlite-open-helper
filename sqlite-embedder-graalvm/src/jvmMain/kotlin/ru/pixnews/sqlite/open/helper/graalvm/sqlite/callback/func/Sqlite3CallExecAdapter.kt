/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore.Sqlite3ExecCallbackId
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.sqlite.open.helper.common.api.plus
import ru.pixnews.sqlite.open.helper.host.memory.readPtr

internal const val SQLITE3_EXEC_CB_FUNCTION_NAME = "sqlite3_exec_cb"

internal class Sqlite3CallExecAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val callbackStore: Sqlite3CallbackStore,
    functionName: String,
    private val logger: Logger = Logger.getLogger(Sqlite3CallExecAdapter::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return callDelegate(
            args[0] as Int,
            args[1] as Int,
            args.asWasmPtr(2),
            args.asWasmPtr(3),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun callDelegate(
        arg1: Int,
        columns: Int,
        pResults: WasmPtr<WasmPtr<Byte>>,
        pColumnNames: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        logger.finest() { "cb() arg1: $arg1 columns: $columns names: $pColumnNames results: $pResults" }
        val delegateId = Sqlite3ExecCallbackId(arg1)
        val delegate = callbackStore.sqlite3ExecCallbacks[delegateId] ?: error("Callback $delegateId not registered")

        val columnNames = (0 until columns).map { columnNo ->
            val ptr: WasmPtr<Byte> = memory.readPtr(pColumnNames + (columnNo * WASM_SIZEOF_PTR.toInt()))
            memory.readNullTerminatedString(ptr)
        }

        val results =  (0 until columns).map { columnNo ->
            val ptr: WasmPtr<Byte> = memory.readPtr(pResults + (columnNo * WASM_SIZEOF_PTR.toInt()))
            memory.readNullTerminatedString(ptr)
        }
        return delegate(columnNames, results)
    }
}