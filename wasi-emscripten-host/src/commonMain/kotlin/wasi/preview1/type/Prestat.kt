/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType

/**
 * Information about a pre-opened capability.
 */
public sealed class Prestat(
    public open val tag: Preopentype,
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
    public data class Dir(
        val prestatDir: PrestatDir,
    ) : Prestat(Preopentype.DIR)
}
