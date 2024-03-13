/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.sqlite.open.helper.host.WasmValueType

/**
 * The contents of a `subscription` when type is type is `eventtype::fd_read` or `eventtype::fd_write`.
 *
 * @param fileDescriptor The file descriptor on which to wait for it to become ready for reading or writing.
 */
public data class SubscriptionFdReadwrite(
    val fileDescriptor: Fd, // (field $file_descriptor $fd)
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
}
