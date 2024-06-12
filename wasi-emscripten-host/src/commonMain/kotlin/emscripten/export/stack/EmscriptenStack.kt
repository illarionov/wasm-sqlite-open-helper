/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory

/**
 * Emscripten Stack bindings.
 *
 * [https://emscripten.org/docs/api_reference/stack.h.html](https://emscripten.org/docs/api_reference/stack.h.html)
 */
public class EmscriptenStack(
    private val exports: EmscriptenStackExports,
    rootLogger: Logger,
) {
    private val logger: Logger = rootLogger.withTag("EmscriptenStack")

    /**
     * Returns the starting address of the stack. This is the address that the stack pointer would point to when
     * no bytes are in use on the stack.
     *
     * Binding for `uintptr_t emscripten_stack_get_base(void)`
     */
    public val emscriptenStackBase: WasmPtr<UInt>
        get() = exports.emscripten_stack_get_base?.executeForPtr() ?: WasmPtr(0)

    /**
     * Returns the end address of the stack.
     * This is the address that the stack pointer would point to when the whole stack is in use.
     * (The address pointed to by the end is not part of the stack itself).
     * Note that the stack grows down so the address returned by [emscriptenStackEnd] is
     * smaller than [emscriptenStackBase].
     *
     * Binding for `uintptr_t emscripten_stack_get_end(void)`
     */
    public val emscriptenStackEnd: WasmPtr<UInt>
        get() = exports.emscripten_stack_get_end?.executeForPtr() ?: WasmPtr(0)

    /**
     * Returns the current stack pointer.
     *
     * Binding for `uintptr_t emscripten_stack_get_current(void)`
     */
    public val emscriptenStackCurrent: WasmPtr<UInt>
        get() = exports.emscripten_stack_get_current.executeForPtr()

    /**
     * Returns the number of free bytes left on the stack. This is required to be fast so that it can be
     * called frequently.
     *
     * Binding for `size_t emscripten_stack_get_free(void)`
     */
    public val emscriptenStackFree: Int
        get() = exports.emscripten_stack_get_free?.executeForInt() ?: 0

    /**
     * Sets the internal values reported by [emscriptenStackBase] and [emscriptenStackEnd].
     * This should only be used by low level libraries such as asyncify fibers.
     *
     * Binding for `void emscripten_stack_set_limits(void* base, void* end)`
     */
    public fun emscriptenStackSetLimits(base: WasmPtr<Unit>, end: WasmPtr<Unit>) {
        exports.emscripten_stack_set_limits.executeVoid(base.addr, end.addr)
    }

    /**
     * Startup routine to initialize the stack.
     *
     * Must be called in the order specified on
     * [https://github.com/emscripten-core/emscripten/blob/main/system/lib/README.md
     * ](https://github.com/emscripten-core/emscripten/blob/main/system/lib/README.md)
     */
    public fun stackCheckInit(memory: Memory) {
        exports.emscripten_stack_init?.let { stackInit ->
            stackInit.executeVoid()
            writeStackCookie(memory)
        }
    }

    public fun setStackLimits() {
        if (exports.emscripten_stack_get_end == null) {
            return
        }
        val stackLow = emscriptenStackBase
        val stackHigh = emscriptenStackEnd
        exports.__set_stack_limits?.executeVoid(stackLow.addr, stackHigh.addr)
            ?: logger.v { "No __set_stack_limits export" }
    }

    @Suppress("MagicNumber")
    public fun writeStackCookie(memory: Memory) {
        if (exports.emscripten_stack_get_end == null) {
            return
        }

        var max = emscriptenStackEnd.addr
        check(max.and(0x03) == 0)

        if (max == 0) {
            max = 4
        }

        memory.writeI32(WasmPtr<Unit>(max), 0x0213_5467)
        memory.writeI32(WasmPtr<Unit>(max + 4), 0x89BA_CDFE_U.toInt())
        memory.writeI32(WasmPtr<Unit>(0), 1_668_509_029)
    }

    @Suppress("MagicNumber")
    public fun checkStackCookie(memory: Memory) {
        if (exports.emscripten_stack_get_end == null) {
            return
        }

        var max = emscriptenStackEnd.addr
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
