/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.sqlite.open.helper.host.WasmValueType
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * Identifiers for preopened capabilities.
 */
public enum class Preopentype(
    public val value: UInt,
) {
    /**
     * A pre-opened directory.
     */
    DIR(0U),

    ;

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = U8
    }
}
