/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.plus

@InternalWasmSqliteHelperApi
public abstract class MemoryRawSource(
    protected var baseAddr: WasmPtr<*>,
    protected val toAddrExclusive: WasmPtr<*>,
) : RawSource {
    private var isClosed: Boolean = false
    protected val bytesLeft: Long
        get() = (toAddrExclusive.addr - baseAddr.addr).toLong()

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount is negative" }
        check(!isClosed) { "Stream is closed" }

        val bytesLeft: Long = (toAddrExclusive.addr - baseAddr.addr).toLong()
        val readBytes = byteCount.coerceAtMost(bytesLeft).toInt()
        if (readBytes <= 0) {
            return -1
        }

        try {
            readBytesFromMemory(baseAddr, sink, readBytes)
        } catch (ise: IllegalStateException) {
            throw ise
        } catch (iae: IllegalArgumentException) {
            throw iae
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            throw IllegalStateException(ex.message, ex)
        }

        baseAddr += readBytes
        return readBytes.toLong()
    }

    protected abstract fun readBytesFromMemory(srcAddr: WasmPtr<*>, sink: Buffer, readBytes: Int)

    override fun close() {
        isClosed = true
    }
}
