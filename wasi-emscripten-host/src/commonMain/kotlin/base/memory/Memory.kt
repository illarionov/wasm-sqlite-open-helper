/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

@InternalWasmSqliteHelperApi
public interface Memory {
    public fun readI8(addr: WasmPtr<*>): Byte
    public fun readI32(addr: WasmPtr<*>): Int
    public fun readI64(addr: WasmPtr<*>): Long
    public fun readBytes(addr: WasmPtr<*>, length: Int): ByteArray

    public fun writeByte(addr: WasmPtr<*>, data: Byte)
    public fun writeI32(addr: WasmPtr<*>, data: Int)
    public fun writeI64(addr: WasmPtr<*>, data: Long)
    public fun write(addr: WasmPtr<*>, data: ByteArray, offset: Int, size: Int)
}

@InternalWasmSqliteHelperApi
public fun Memory.readU8(addr: WasmPtr<*>): UByte = readI8(addr).toUByte()

@InternalWasmSqliteHelperApi
public fun Memory.readU32(addr: WasmPtr<*>): UInt = readI32(addr).toUInt()

@InternalWasmSqliteHelperApi
public fun Memory.readU64(addr: WasmPtr<*>): ULong = readI64(addr).toULong()

@Suppress("UNCHECKED_CAST")
@InternalWasmSqliteHelperApi
public fun <T : Any, P : WasmPtr<T>> Memory.readPtr(addr: WasmPtr<P>): P = WasmPtr<T>(readI32(addr)) as P

@InternalWasmSqliteHelperApi
public fun Memory.writePtr(addr: WasmPtr<*>, data: WasmPtr<*>): Unit = writeI32(addr, data.addr)

@InternalWasmSqliteHelperApi
public fun Memory.write(addr: WasmPtr<*>, data: ByteArray): Unit = write(addr, data, 0, data.size)
