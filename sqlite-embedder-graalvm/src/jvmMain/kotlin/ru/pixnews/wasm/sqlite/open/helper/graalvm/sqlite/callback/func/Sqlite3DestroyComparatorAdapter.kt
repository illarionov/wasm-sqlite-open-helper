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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore

internal const val SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME = "sqlite3_comparator_destroy"

@Suppress("UnusedPrivateProperty")
internal class Sqlite3DestroyComparatorAdapter(
    language: WasmLanguage,
    module: WasmModule,
    private val callbackStore: Sqlite3CallbackStore,
    host: SqliteEmbedderHost,
    functionName: String,
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance) {
        val args = frame.arguments
        destroyComparator(Sqlite3CallbackStore.Sqlite3ComparatorId(args.getArgAsInt(0)))
    }

    @CompilerDirectives.TruffleBoundary
    private fun destroyComparator(
        comparatorId: Sqlite3CallbackStore.Sqlite3ComparatorId,
    ) {
        logger.v { "Removing comparator $comparatorId" }
        callbackStore.sqlite3Comparators.remove(comparatorId)
    }
}
