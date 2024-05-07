/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.function

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.SqliteComparatorFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

internal class Sqlite3ComparatorAdapter(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    comparatorStore: (SqliteComparatorId) -> SqliteComparatorCallback?,
) : BaseWasmNode<SqliteComparatorFunctionHandle>(
    language,
    module,
    SqliteComparatorFunctionHandle(host, comparatorStore),
) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return invokeComparator(
            memory(frame),
            args.getArgAsInt(0),
            args.getArgAsInt(1),
            args.getArgAsWasmPtr(2),
            args.getArgAsInt(3),
            args.getArgAsWasmPtr(4),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeComparator(
        memory: WasmMemory,
        comparatorId: Int,
        str1Size: Int,
        str1: WasmPtr<Byte>,
        str2Size: Int,
        str2: WasmPtr<Byte>,
    ): Int = handle.execute(memory.toHostMemory(), comparatorId, str1Size, str1, str2Size, str2)
}
