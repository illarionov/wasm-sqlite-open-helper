/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.sqlite.open.helper.host.WasmValueType

/**
 * Flags provided to `sock_send`. As there are currently no flags
 * defined, it must be set to zero.
 */
@JvmInline
public value class SiFlags(
    public val rawValue: UInt,
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasiValueTypes.U16
    }
}
