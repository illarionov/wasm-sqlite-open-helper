/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * Which channels on a socket to shut down.
 */
@JvmInline
public value class Sdflags(
    public val rawMask: UByte,
) {
    public constructor(
        vararg flags: Sdflags,
    ) : this(
        flags.fold(0.toUByte()) { acc, flag -> acc.or(flag.mask) },
    )

    public enum class Sdflags(
        public val mask: UByte,
    ) {
        /**
         * Disables further receive operations.
         */
        RD(0),

        /**
         * Disables further send operations.
         */
        WR(1),

        ;

        constructor(bit: Int) : this(1U.shl(bit).toUByte())
    }

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = U8
    }
}
