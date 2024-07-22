/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

@InternalWasmSqliteHelperApi
public abstract class MemoryRawSink(
    protected var baseAddr: WasmPtr<*>,
    protected val toAddrExclusive: WasmPtr<*>,
) : RawSink {
    private var isClosed: Boolean = false

    override fun write(source: Buffer, byteCount: Long) {
        require(byteCount >= 0) { "byteCount is negative" }
        checkSinkNotClosed()

        val endAddrExclusive = getEndAddressOrThrow(byteCount)

        try {
            writeBytesToMemory(source, baseAddr, byteCount)
        } catch (ise: IllegalStateException) {
            throw ise
        } catch (iae: IllegalArgumentException) {
            throw iae
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            throw IllegalStateException(ex.message, ex)
        }
        baseAddr = WasmPtr<Unit>(endAddrExclusive.toInt())
    }

    protected abstract fun writeBytesToMemory(
        source: Buffer,
        toAddr: WasmPtr<*>,
        byteCount: Long,
    )

    override fun flush() {
        checkSinkNotClosed()
        // no-op
    }

    override fun close() {
        isClosed = true
    }

    private fun checkSinkNotClosed(): Unit = check(!isClosed) { "Sink is closed" }

    protected fun getEndAddressOrThrow(
        byteCount: Long,
    ): Long {
        val endAddrExclusive = baseAddr.addr + byteCount
        require(endAddrExclusive <= toAddrExclusive.addr) {
            "Cannot write `$byteCount` bytes to memory range [$baseAddr .. $toAddrExclusive): out of boundary access"
        }
        return endAddrExclusive
    }
}
