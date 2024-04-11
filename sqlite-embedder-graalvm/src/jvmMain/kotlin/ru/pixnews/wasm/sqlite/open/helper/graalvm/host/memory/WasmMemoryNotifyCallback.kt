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
        logger.v { "execute(): $addr $count" }

        val waiterListRecord = waitersStore.getListForIndex((addr * 4).toInt())
        return waiterListRecord.notifyWaiters(count)
    }
}
