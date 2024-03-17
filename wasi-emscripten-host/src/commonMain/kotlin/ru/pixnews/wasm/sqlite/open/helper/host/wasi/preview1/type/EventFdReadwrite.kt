/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType

/**
 * The contents of an `event` when type is `eventtype::fd_read` or
 * `eventtype::fd_write`.
 *
 * @param nbytes The number of bytes available for reading or writing.
 * @param flags The state of the file descriptor.
 */
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT")
public data class EventFdReadwrite(
    val nbytes: FileSize, // (field $nbytes $filesize)
    val flags: Eventrwflags, // field $flags $eventrwflags)
) {
    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = WasiValueTypes.U32
    }
}
