/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "UnusedPrivateProperty", "BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.sqlite.open.helper.chasm.bindings

import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.member
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteMemoryBindings
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenInitializer

internal class ChasmSqliteMemoryBindings(
    instance: ChasmInstance,
) : SqliteMemoryBindings {
    val malloc by instance.member()
    val free by instance.member()
    val realloc by instance.member()

    val stackSave by instance.member()
    val stackRestore by instance.member()
    val stackAlloc by instance.member()

    private val emscripten_builtin_memalign by instance.member()
    private val emscripten_stack_init by instance.member()
    private val emscripten_stack_get_free by instance.member()
    private val emscripten_stack_get_base by instance.member()
    private val emscripten_stack_get_end by instance.member()
    private val emscripten_stack_get_current by instance.member()

    private val sqlite3_malloc by instance.member()
    private val sqlite3_free by instance.member()
    private val sqlite3_realloc by instance.member()
    private val sqlite3_malloc64 by instance.member()
    private val sqlite3_realloc64 by instance.member()

    // https://github.com/emscripten-core/emscripten/blob/main/system/lib/README.md
    fun init(memory: Memory) {
        // TODO: check method exists
        EmscriptenInitializer(memory, emscripten_stack_init, emscripten_stack_get_end).init()
    }

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
