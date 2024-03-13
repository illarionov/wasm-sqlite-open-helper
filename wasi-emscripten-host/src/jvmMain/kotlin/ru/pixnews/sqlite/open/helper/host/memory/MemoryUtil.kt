/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.memory

import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.common.api.isSqlite3Null
import java.io.ByteArrayOutputStream

public fun Memory.readNullableZeroTerminatedString(offset: WasmPtr<Byte>): String? {
    return if (!offset.isSqlite3Null()) {
        readZeroTerminatedString(offset)
    } else {
        null
    }
}

public fun Memory.readZeroTerminatedString(offset: WasmPtr<Byte>): String {
    check(offset.addr != 0)
    val mem = ByteArrayOutputStream()
    var addr = offset.addr
    do {
        val byte = this.readI8(WasmPtr<Unit>(addr))
        addr += 1
        if (byte == 0.toByte()) {
            break
        }
        mem.write(byte.toInt())
    } while (true)

    return mem.toString("UTF-8")
}

public fun Memory.writeZeroTerminatedString(
    offset: WasmPtr<*>,
    value: String,
): Int {
    val encoded = value.encodeToByteArray()
    write(offset, encoded)
    writeByte(WasmPtr<Unit>(offset.addr + encoded.size), 0)
    return encoded.size + 1
}

public fun String.encodeToNullTerminatedByteArray(): ByteArray {
    val os = ByteArrayOutputStream(this.length)
    os.writeBytes(this.encodeToByteArray())
    os.write(0)
    return os.toByteArray()
}

public fun String.encodedStringLength(): Int = this.encodeToByteArray().size

public fun String.encodedNullTerminatedStringLength(): Int = this.encodeToByteArray().size + 1
