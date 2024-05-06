/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * File or memory access pattern advisory information.
 */
public enum class Advice(
    public val id: Int,
) {
    /**
     * The application has no advice to give on its behavior with respect to the specified data.
     */
    NORMAL(0),

    /**
     * The application expects to access the specified data sequentially from lower offsets to higher offsets.
     */
    SEQUENTIAL(1),

    /**
     * The application expects to access the specified data in a random order.
     */
    RANDOM(2),

    /**
     * The application expects to access the specified data in the near future.
     */
    WILLNEED(3),

    /**
     * The application expects that it will not access the specified data in the near future.
     */
    DONTNEED(4),

    /**
     * The application expects to access the specified data once and then not reuse it thereafter.
     */
    NOREUSE(5),

    ;

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = U8
    }
}
