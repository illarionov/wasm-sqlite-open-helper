/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.ReadOnlyMemory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory.DynamicMemory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory.freeSilent
import ru.pixnews.wasm.sqlite.open.helper.host.include.pthread_t

public class EmscriptenPthread(
    private val exports: EmscriptenPthreadExports,
    private val dynamicMemory: DynamicMemory,
    private val memory: ReadOnlyMemory,
) {
    public fun pthreadSelf(): pthread_t {
        return exports.pthread_self.executeForLong().toULong()
    }

    public fun pthreadCreate(
        attr: WasmPtr<*>,
        startRoutine: IndirectFunctionTableIndex,
        arg: WasmPtr<*>,
    ): pthread_t {
        var threadIdRef: WasmPtr<pthread_t> = WasmPtr.sqlite3Null()
        try {
            threadIdRef = dynamicMemory.allocOrThrow(8U)

            val errNo = requireNotNull(exports.pthread_create) {
                "pthread_create not exported." +
                        " Recompile application with _pthread_create and _pthread_exit in EXPORTED_FUNCTIONS"
            }.executeForInt(threadIdRef.addr, attr.addr, startRoutine.funcId, arg.addr)

            if (errNo != 0) {
                throw PthreadException("pthread_create() failed with error $errNo", errNo)
            }

            return memory.readI32(threadIdRef).toULong()
        } finally {
            dynamicMemory.freeSilent(threadIdRef)
        }
    }

    public fun pthreadExit(
        retval: WasmPtr<*>,
    ) {
        requireNotNull(exports.pthread_exit) {
            "pthread_exit not exported." +
                    " Recompile application with _pthread_create and _pthread_exit in EXPORTED_FUNCTIONS"
        }.executeVoid(retval.addr)
    }
}
