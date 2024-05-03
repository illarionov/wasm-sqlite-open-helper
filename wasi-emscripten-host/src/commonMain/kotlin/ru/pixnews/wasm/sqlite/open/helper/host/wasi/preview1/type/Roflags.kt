/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import kotlin.jvm.JvmInline

/**
 * Flags returned by `sock_recv`.
 */
@JvmInline
public value class Roflags(
    public val rawMask: UShort,
) {
    public constructor(
        vararg flags: RoflagsValue,
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) },
    )

    public enum class RoflagsValue(
        public val mask: UShort,
    ) {
        /**
         * Returned by `sock_recv`: Message data has been truncated.
         */
        RECV_DATA_TRUNCATED(0),

        ;

        constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = WasiValueTypes.U16
    }
}
