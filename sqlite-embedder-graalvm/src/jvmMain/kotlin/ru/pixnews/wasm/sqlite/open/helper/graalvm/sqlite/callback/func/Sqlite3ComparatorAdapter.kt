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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

internal const val SQLITE3_COMPARATOR_CALL_FUNCTION_NAME = "sqlite3_comparator_call_cb"

internal class Sqlite3ComparatorAdapter(
    language: WasmLanguage,
    module: WasmModule,
    private val callbackStore: Sqlite3CallbackStore,
    host: SqliteEmbedderHost,
    functionName: String,
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return invokeComparator(
            memory(frame),
            Sqlite3CallbackStore.Sqlite3ComparatorId(args.getArgAsInt(0)),
            args.getArgAsInt(1),
            args.getArgAsWasmPtr(2),
            args.getArgAsInt(3),
            args.getArgAsWasmPtr(4),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeComparator(
        memory: WasmMemory,
        comparatorId: Sqlite3CallbackStore.Sqlite3ComparatorId,
        str1Size: Int,
        str1: WasmPtr<Byte>,
        str2Size: Int,
        str2: WasmPtr<Byte>,
    ): Int {
        logger.v { "invokeComparator() db: $comparatorId" }
        val hostMemory = memory.toHostMemory()
        val delegate: SqliteComparatorCallback = callbackStore.sqlite3Comparators[comparatorId]
            ?: error("Comparator $comparatorId not registered")

        val str1Bytes = hostMemory.readBytes(str1, str1Size)
        val str2Bytes = hostMemory.readBytes(str2, str2Size)

        return delegate.invoke(String(str1Bytes), String(str2Bytes))
    }
}
