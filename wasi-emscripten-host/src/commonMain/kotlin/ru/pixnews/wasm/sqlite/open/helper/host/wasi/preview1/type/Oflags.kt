/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import kotlin.jvm.JvmInline

/**
 * Open flags used by `path_open`.
 */
@JvmInline
public value class Oflags(
    public val rawMask: UShort,
) {
    public constructor(
        vararg flags: Oflags,
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) },
    )

    public enum class Oflags(
        public val mask: UShort,
    ) {
        /**
         * Create file if it does not exist.
         */
        CREAT(0),

        /**
         * Fail if not a directory.
         */
        DIRECTORY(1),

        /**
         * Fail if file already exists.
         */
        EXCL(2),

        /**
         * Truncate file to size 0.
         */
        TRUNC(3),

        ;

        constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = WasiValueTypes.U16
    }
}
