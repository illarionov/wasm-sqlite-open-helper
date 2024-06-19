/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.isSqlite3Null

@InternalWasmSqliteHelperApi
public class DynamicMemory(
    public val exports: DynamicMemoryExports,
) {
    public fun <P : Any?> allocOrThrow(len: UInt): WasmPtr<P> {
        check(len > 0U)
        val mem: WasmPtr<P> = requireNotNull(exports.malloc) {
            functionNotExported("malloc")
        }.executeForPtr(len.toInt())

        if (mem.isSqlite3Null()) {
            throw OutOfMemoryException()
        }

        return mem
    }

    public fun free(ptr: WasmPtr<*>) {
        requireNotNull(exports.free) {
            functionNotExported("free")
        }.executeVoid(ptr)
    }

    private fun functionNotExported(function: String): String = "$function function is not exported. " +
            "Recompile application with \"_malloc\" and \"_free\" in EXPORTED_FUNCTIONS"

    public class OutOfMemoryException : RuntimeException()
}

@InternalWasmSqliteHelperApi
public fun DynamicMemory.freeSilent(value: WasmPtr<*>): Result<Unit> = kotlin.runCatching {
    free(value)
}
