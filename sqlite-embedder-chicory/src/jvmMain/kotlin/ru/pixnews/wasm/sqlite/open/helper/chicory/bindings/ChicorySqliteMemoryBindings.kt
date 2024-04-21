/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "UnusedPrivateProperty", "BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.sqlite.open.helper.chicory.bindings

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.exceptions.ChicoryException
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.member
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteMemoryBindings
import ru.pixnews.wasm.sqlite.open.helper.host.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenInitializer
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

internal class ChicorySqliteMemoryBindings(
    mainBindings: Instance,
) : SqliteMemoryBindings {
    val malloc by mainBindings.member()
    val free by mainBindings.member()
    val realloc by mainBindings.member()

    val stackSave by mainBindings.member()
    val stackRestore by mainBindings.member()
    val stackAlloc by mainBindings.member()

    private val emscripten_builtin_memalign by mainBindings.member()
    private val emscripten_stack_init: WasmFunctionBinding? = try {
        ChicoryWasmFunctionBinding(mainBindings.export("emscripten_stack_init"))
    } catch (@Suppress("SwallowedException") ex: ChicoryException) {
        null
    }
    private val emscripten_stack_get_free by mainBindings.member()
    private val emscripten_stack_get_base by mainBindings.member()
    private val emscripten_stack_get_end by mainBindings.member()
    private val emscripten_stack_get_current by mainBindings.member()

    private val sqlite3_malloc by mainBindings.member()
    private val sqlite3_free by mainBindings.member()
    private val sqlite3_realloc by mainBindings.member()
    private val sqlite3_malloc64 by mainBindings.member()
    private val sqlite3_realloc64 by mainBindings.member()

    // https://github.com/emscripten-core/emscripten/blob/main/system/lib/README.md
    fun init(memory: Memory) {
        if (emscripten_stack_init != null) {
            EmscriptenInitializer(memory, emscripten_stack_init, emscripten_stack_get_end).init()
        }
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
