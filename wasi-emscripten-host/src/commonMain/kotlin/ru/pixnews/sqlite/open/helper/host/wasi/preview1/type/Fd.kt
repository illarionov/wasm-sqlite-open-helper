/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.sqlite.open.helper.host.WasmValueType

/**
 * A file descriptor handle.
 */
@JvmInline
public value class Fd(
    public val fd: Int,
) {
    override fun toString(): String = "Fd($fd)"
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasiValueTypes.Handle
    }
}
