/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType

/**
 * The contents of a `prestat` when type is `preopentype::dir`.
 *
 * @param prNameLen The length of the directory name for use with `fd_prestat_dir_name`.
 */
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT")
public data class PrestatDir(
    public val prNameLen: Size, // (field $pr_name_len $size)
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
}
