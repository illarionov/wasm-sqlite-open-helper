/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import okio.Buffer
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.isSqlite3Null

@InternalWasmSqliteHelperApi
public interface Memory {
    public fun readI8(addr: WasmPtr<*>): Byte
    public fun readI32(addr: WasmPtr<*>): Int
    public fun readI64(addr: WasmPtr<*>): Long
    public fun read(
        addr: WasmPtr<*>,
        destination: ByteArray,
        destinationOffset: Int = 0,
        readBytes: Int = destination.size,
    )

    public fun writeI8(addr: WasmPtr<*>, data: Byte)
    public fun writeI32(addr: WasmPtr<*>, data: Int)
    public fun writeI64(addr: WasmPtr<*>, data: Long)
    public fun write(
        addr: WasmPtr<*>,
        source: ByteArray,
        sourceOffset: Int = 0,
        writeBytes: Int = source.size,
    )
}

@InternalWasmSqliteHelperApi
public fun Memory.readU8(addr: WasmPtr<*>): UByte = readI8(addr).toUByte()

@InternalWasmSqliteHelperApi
public fun Memory.readU32(addr: WasmPtr<*>): UInt = readI32(addr).toUInt()

@InternalWasmSqliteHelperApi
public fun Memory.readU64(addr: WasmPtr<*>): ULong = readI64(addr).toULong()

@InternalWasmSqliteHelperApi
public fun Memory.writeU8(addr: WasmPtr<*>, data: UByte): Unit = writeI8(addr, data.toByte())

@InternalWasmSqliteHelperApi
public fun Memory.writeU32(addr: WasmPtr<*>, data: UInt): Unit = writeI32(addr, data.toInt())

@InternalWasmSqliteHelperApi
public fun Memory.writeU64(addr: WasmPtr<*>, data: ULong): Unit = writeI64(addr, data.toLong())

@Suppress("UNCHECKED_CAST")
@InternalWasmSqliteHelperApi
public fun <T : Any, P : WasmPtr<T>> Memory.readPtr(addr: WasmPtr<P>): P = WasmPtr<T>(readI32(addr)) as P

@InternalWasmSqliteHelperApi
public fun Memory.writePtr(addr: WasmPtr<*>, data: WasmPtr<*>): Unit = writeI32(addr, data.addr)

@InternalWasmSqliteHelperApi
public fun Memory.readNullableNullTerminatedString(offset: WasmPtr<Byte>): String? {
    return if (!offset.isSqlite3Null()) {
        readNullTerminatedString(offset)
    } else {
        null
    }
}

@InternalWasmSqliteHelperApi
public fun Memory.readNullTerminatedString(offset: WasmPtr<Byte>): String {
    check(offset.addr != 0)

    val mem = Buffer()
    var addr = offset.addr
    do {
        val byte = this.readI8(WasmPtr<Unit>(addr))
        addr += 1
        if (byte == 0.toByte()) {
            break
        }
        mem.writeByte(byte.toInt())
    } while (true)

    return mem.readByteString().utf8()
}

@InternalWasmSqliteHelperApi
public fun Memory.writeNullTerminatedString(
    offset: WasmPtr<*>,
    value: String,
): Int {
    val encoded = value.encodeToByteArray()
    write(offset, encoded)
    writeI8(WasmPtr<Unit>(offset.addr + encoded.size), 0)
    return encoded.size + 1
}
