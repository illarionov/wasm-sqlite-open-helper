/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "UnusedPrivateProperty", "BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.sqlite.open.helper.chasm.exports

import ru.pixnews.wasm.sqlite.open.helper.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.functionMember
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.chasm.platform.throwOutOfMemoryError
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteDynamicMemoryExports
import ru.pixnews.wasm.sqlite.open.helper.isNull

internal class ChasmSqliteDynamicMemoryExports(
    instance: ChasmInstance,
) : SqliteDynamicMemoryExports {
    private val sqlite3_malloc by instance.functionMember()
    private val sqlite3_free by instance.functionMember()
    private val sqlite3_realloc by instance.functionMember()
    private val sqlite3_malloc64 by instance.functionMember()
    private val sqlite3_realloc64 by instance.functionMember()

    override fun <P : Any?> sqliteAllocOrThrow(len: UInt): WasmPtr<P> {
        check(len > 0U)
        val mem: WasmPtr<P> = WasmPtr(sqlite3_malloc.executeForPtr(len.toInt()))

        if (mem.isNull()) {
            throwOutOfMemoryError()
        }

        return mem
    }

    override fun sqliteFree(ptr: WasmPtr<*>) {
        sqlite3_free.executeVoid(ptr)
    }
}
