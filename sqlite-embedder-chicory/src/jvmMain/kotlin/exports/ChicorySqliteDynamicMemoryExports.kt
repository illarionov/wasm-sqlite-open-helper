/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "UnusedPrivateProperty", "BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.sqlite.open.helper.chicory.exports

import com.dylibso.chicory.runtime.Instance
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.functionMember
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteDynamicMemoryExports
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.isSqlite3Null

internal class ChicorySqliteDynamicMemoryExports(
    mainBindings: Instance,
) : SqliteDynamicMemoryExports {
    private val sqlite3_malloc by mainBindings.functionMember()
    private val sqlite3_free by mainBindings.functionMember()
    private val sqlite3_realloc by mainBindings.functionMember()
    private val sqlite3_malloc64 by mainBindings.functionMember()
    private val sqlite3_realloc64 by mainBindings.functionMember()

    override fun <P : Any?> sqliteAllocOrThrow(len: UInt): WasmPtr<P> {
        check(len > 0U)
        val mem: WasmPtr<P> = sqlite3_malloc.executeForPtr(len.toInt())

        if (mem.isSqlite3Null()) {
            throw OutOfMemoryError()
        }

        return mem
    }

    override fun sqliteFree(ptr: WasmPtr<*>) {
        sqlite3_free.executeVoid(ptr)
    }
}
