/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.ext

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import java.io.ByteArrayOutputStream

internal fun String.encodeToNullTerminatedByteArray(): ByteArray {
    val os = ByteArrayOutputStream(this.length)
    this.encodeToByteArray().let { os.write(it, 0, it.size) }
    os.write(0)
    return os.toByteArray()
}

// TODO: merge with MemoryUtil
@InternalWasmSqliteHelperApi
internal fun String.encodedNullTerminatedStringLength(): Int = this.encodeToByteArray().size + 1
