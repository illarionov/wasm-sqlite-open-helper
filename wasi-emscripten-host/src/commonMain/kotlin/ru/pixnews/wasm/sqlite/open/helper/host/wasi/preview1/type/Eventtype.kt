/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * Type of a subscription to an event or its occurrence.
 */
public enum class Eventtype(
    public val id: Int,
) {
    /**
     * The time value of clock `subscription_clock::id` has
     * reached timestamp `subscription_clock::timeout`.
     */
    CLOCK(0),

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has data
     * available for reading. This event always triggers for regular files.
     */
    FD_READ(1),

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has capacity
     * available for writing. This event always triggers for regular files.
     */
    FD_WRITE(2),

    ;

    constructor(id: Long) : this(id.toInt())

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = U8
    }
}
