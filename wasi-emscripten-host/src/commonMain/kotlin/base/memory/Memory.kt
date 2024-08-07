/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.writeString
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

@InternalWasmSqliteHelperApi
public interface Memory : ReadOnlyMemory {
    public fun writeI8(addr: WasmPtr<*>, data: Byte)
    public fun writeI32(addr: WasmPtr<*>, data: Int)
    public fun writeI64(addr: WasmPtr<*>, data: Long)

    public fun sink(fromAddr: WasmPtr<*>, toAddrExclusive: WasmPtr<*>): RawSink
}

@InternalWasmSqliteHelperApi
public fun Memory.writeU8(addr: WasmPtr<*>, data: UByte): Unit = writeI8(addr, data.toByte())

@InternalWasmSqliteHelperApi
public fun Memory.writeU32(addr: WasmPtr<*>, data: UInt): Unit = writeI32(addr, data.toInt())

@InternalWasmSqliteHelperApi
public fun Memory.writeU64(addr: WasmPtr<*>, data: ULong): Unit = writeI64(addr, data.toLong())

@InternalWasmSqliteHelperApi
public fun Memory.writePtr(addr: WasmPtr<*>, data: WasmPtr<*>): Unit = writeI32(addr, data.addr)

@InternalWasmSqliteHelperApi
public fun Memory.writeNullTerminatedString(
    offset: WasmPtr<*>,
    value: String,
): Int {
    val buffer = Buffer().apply {
        writeString(value)
        writeByte(0)
    }
    val size = buffer.size.toInt()
    sinkWithMaxSize(offset, size).use {
        it.write(buffer, size.toLong())
    }
    return size
}

@InternalWasmSqliteHelperApi
public fun <P : Any?> Memory.sinkWithMaxSize(
    fromAddr: WasmPtr<P>,
    maxSize: Int,
): RawSink = sink(fromAddr, WasmPtr<P>(fromAddr.addr + maxSize))
