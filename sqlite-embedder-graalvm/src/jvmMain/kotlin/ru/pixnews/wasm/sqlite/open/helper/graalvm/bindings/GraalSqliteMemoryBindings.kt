/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteMemoryBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.member
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

@Suppress("VariableNaming", "MagicNumber", "UnusedPrivateProperty", "BLANK_LINE_BETWEEN_PROPERTIES")
internal class GraalSqliteMemoryBindings(
    mainBindings: Value,
) : SqliteMemoryBindings {
    val malloc by mainBindings.member()
    val free by mainBindings.member()
    val realloc by mainBindings.member()

    val stackSave by mainBindings.member()
    val stackRestore by mainBindings.member()
    val stackAlloc by mainBindings.member()

    private val emscripten_builtin_memalign by mainBindings.member()
    private val emscripten_stack_init: WasmFunctionBinding? = mainBindings
        .getMember("emscripten_stack_init")
        ?.let(::GraalWasmFunctionBinding)
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

    private class EmscriptenInitializer(
        private val memory: Memory,
        private val emscriptenStackInit: WasmFunctionBinding,
        private val emscriptenStackGetEnd: WasmFunctionBinding,
    ) {
        fun init() {
            emscriptenStackInit.executeVoid()
            writeStackCookie()
            checkStackCookie()
        }

        private fun writeStackCookie() {
            var max = emscriptenStackGetEnd.executeForInt()
            check(max.and(0x03) == 0)

            if (max == 0) {
                max = 4
            }

            memory.writeI32(WasmPtr<Unit>(max), 0x0213_5467)
            memory.writeI32(WasmPtr<Unit>(max + 4), 0x89BA_CDFE_U.toInt())
            memory.writeI32(WasmPtr<Unit>(0), 1_668_509_029)
        }

        private fun checkStackCookie() {
            var max = emscriptenStackGetEnd.executeForInt()
            check(max.and(0x03) == 0)

            if (max == 0) {
                max = 4
            }

            val cookie1 = memory.readI32(WasmPtr<Unit>(max))
            val cookie2 = memory.readI32(WasmPtr<Unit>(max + 4))

            check(cookie1 == 0x0213_5467 && cookie2 == 0x89BA_CDFE_U.toInt()) {
                "Stack overflow! Stack cookie has been overwritten at ${max.toString(16)}, expected hex dwords " +
                        "0x89BACDFE and 0x2135467, but received ${cookie2.toString(16)} ${cookie2.toString(16)}"
            }
        }
    }
}
