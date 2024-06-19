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
    writeByte(WasmPtr<Unit>(offset.addr + encoded.size), 0)
    return encoded.size + 1
}
