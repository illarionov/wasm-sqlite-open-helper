/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include

import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask
import kotlin.jvm.JvmInline

/**
 * File mode bits (mode_t)
 */
@JvmInline
public value class FileMode(
    public override val mask: UInt,
) : SqliteUintBitMask<FileMode> {
    override val newInstance: (UInt) -> FileMode get() = ::FileMode
    override fun toString(): String = fileModeTypeToString(mask)
}

internal fun fileModeTypeToString(mask: UInt): String = "0${mask.toString(8)}"
