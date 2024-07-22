/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.readString
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.isSqlite3Null

@InternalWasmSqliteHelperApi
public interface ReadOnlyMemory {
    public fun readI8(addr: WasmPtr<*>): Byte
    public fun readI32(addr: WasmPtr<*>): Int
    public fun readI64(addr: WasmPtr<*>): Long

    public fun source(fromAddr: WasmPtr<*>): RawSource
    public fun read(
        fromAddr: WasmPtr<*>,
        toSink: RawSink,
        readBytes: Int,
    )
}

@InternalWasmSqliteHelperApi
public fun ReadOnlyMemory.readU8(addr: WasmPtr<*>): UByte = readI8(addr).toUByte()

@InternalWasmSqliteHelperApi
public fun ReadOnlyMemory.readU32(addr: WasmPtr<*>): UInt = readI32(addr).toUInt()

@InternalWasmSqliteHelperApi
public fun ReadOnlyMemory.readU64(addr: WasmPtr<*>): ULong = readI64(addr).toULong()

@Suppress("UNCHECKED_CAST")
@InternalWasmSqliteHelperApi
public fun <T : Any, P : WasmPtr<T>> ReadOnlyMemory.readPtr(addr: WasmPtr<P>): P = WasmPtr<T>(readI32(addr)) as P

@InternalWasmSqliteHelperApi
public fun ReadOnlyMemory.readNullableNullTerminatedString(offset: WasmPtr<Byte>): String? {
    return if (!offset.isSqlite3Null()) {
        readNullTerminatedString(offset)
    } else {
        null
    }
}

@InternalWasmSqliteHelperApi
public fun ReadOnlyMemory.readNullTerminatedString(offset: WasmPtr<Byte>): String {
    check(offset.addr != 0)

    val mem = Buffer()
    var addr = offset.addr
    do {
        val byte = this.readI8(WasmPtr<Unit>(addr))
        addr += 1
        if (byte == 0.toByte()) {
            break
        }
        mem.writeByte(byte)
    } while (true)

    return mem.readString()
}
