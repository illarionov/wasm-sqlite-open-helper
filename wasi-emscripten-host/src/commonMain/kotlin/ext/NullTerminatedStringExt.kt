/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.ext

import kotlinx.io.Buffer
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public fun String.encodeToNullTerminatedBuffer(
    truncateAtSize: Int = Int.MAX_VALUE,
): Buffer = Buffer().also { buffer ->
    buffer.writeNullTerminatedString(this, truncateAtSize)
}

@InternalWasmSqliteHelperApi
public fun String.encodedNullTerminatedStringLength(): Int = this.encodeToByteArray().size + 1
