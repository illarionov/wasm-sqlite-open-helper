/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType

/**
 * A region of memory for scatter/gather writes.
 *
 * @param buf The address of the buffer to be written.
 * @param bufLen The length of the buffer to be written.
 */
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT")
public data class CioVec(
    val buf: WasmPtr<Byte>, // (@witx const_pointer u8))
    val bufLen: Size, // (field $buf_len $size)
) {
    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType =
            WasiValueTypes.U32
    }
}
