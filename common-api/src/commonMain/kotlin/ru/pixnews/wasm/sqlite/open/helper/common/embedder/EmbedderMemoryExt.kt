/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.common.embedder

import okio.Buffer
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null

@InternalWasmSqliteHelperApi
public fun EmbedderMemory.readNullableZeroTerminatedString(offset: WasmPtr<Byte>): String? {
    return if (!offset.isSqlite3Null()) {
        readZeroTerminatedString(offset)
    } else {
        null
    }
}

@InternalWasmSqliteHelperApi
public fun EmbedderMemory.readZeroTerminatedString(offset: WasmPtr<Byte>): String {
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
public fun EmbedderMemory.writeZeroTerminatedString(
    offset: WasmPtr<*>,
    value: String,
): Int {
    val encoded = value.encodeToByteArray()
    write(offset, encoded)
    writeByte(WasmPtr<Unit>(offset.addr + encoded.size), 0)
    return encoded.size + 1
}

@InternalWasmSqliteHelperApi
public fun String.encodeToNullTerminatedByteArray(
    maxLength: Int = Int.MAX_VALUE,
): ByteArray {
    check(maxLength > 0)

    val raw = this.encodeToByteArray()
    val rawSize = raw.size.coerceAtMost(maxLength - 1)
    val os = ByteArray(rawSize + 1) { pos ->
        if (pos < rawSize) {
            raw[pos]
        } else {
            0
        }
    }
    os[rawSize - 1] = 0
    return os
}

@InternalWasmSqliteHelperApi
public fun String.encodedStringLength(): Int = this.encodeToByteArray().size

@InternalWasmSqliteHelperApi
public fun String.encodedNullTerminatedStringLength(): Int = this.encodeToByteArray().size + 1
