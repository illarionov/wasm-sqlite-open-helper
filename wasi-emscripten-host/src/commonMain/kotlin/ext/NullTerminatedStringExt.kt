/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.ext

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public fun String.encodeToNullTerminatedByteArray(
    truncateAtSize: Int = Int.MAX_VALUE,
): ByteArray {
    require(truncateAtSize > 0)

    val raw = this.encodeToByteArray()
    val rawSize = raw.size.coerceAtMost(truncateAtSize - 1)
    val os = ByteArray(rawSize + 1) { pos ->
        if (pos < rawSize) {
            raw[pos]
        } else {
            0
        }
    }
    os[rawSize] = 0
    return os
}

@InternalWasmSqliteHelperApi
public fun String.encodedNullTerminatedStringLength(): Int = this.encodeToByteArray().size + 1
