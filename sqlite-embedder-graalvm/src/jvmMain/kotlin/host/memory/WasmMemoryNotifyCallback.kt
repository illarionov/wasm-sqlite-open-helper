/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import com.oracle.truffle.api.interop.InteropLibrary
import com.oracle.truffle.api.interop.TruffleObject
import com.oracle.truffle.api.library.ExportLibrary
import com.oracle.truffle.api.library.ExportMessage
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsLong

/**
 * Сallback to be called from the GraalVM Wasm runtime when atomic_notify Wasm function is executed.
 *
 * @see <a
 *   href="https://github.com/WebAssembly/threads/blob/main/proposals/threads/Overview.md#wait-and-notify-operators"
 * >WebAssembly wait and notify operators</a>
 *
 */
@InternalWasmSqliteHelperApi
@ExportLibrary(InteropLibrary::class)
internal class WasmMemoryNotifyCallback(
    private val waitersStore: SharedMemoryWaiterListStore,
    logger: Logger,
) : TruffleObject {
    private val logger = logger.withTag("WasmMemoryNotifyCallback")

    @Suppress("FunctionOnlyReturningConstant")
    @ExportMessage
    fun isExecutable(): Boolean {
        return true
    }

    @ExportMessage
    fun execute(arguments: Array<Any>): Any {
        val addr = arguments.getArgAsLong(0)
        val count = arguments.getArgAsInt(1)
        logger.v { "atomic_notify(addr = 0x${addr.toString(16)}, count = $count)" }

        val waiterListRecord = waitersStore.getListForIndex((addr * 4).toInt())
        return waiterListRecord.notifyWaiters(count)
    }
}
