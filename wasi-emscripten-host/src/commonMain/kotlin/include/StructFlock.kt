/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include

import kotlinx.io.Source
import kotlinx.io.readIntLe
import kotlinx.io.readLongLe
import kotlinx.io.readShortLe
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence

/**
 * <include/fcntl.h> struct flock
 */
@Suppress("PropertyName", "ConstructorParameterNaming")
public data class StructFlock(
    val l_type: Short,
    val l_whence: Short,
    val l_start: off_t,
    val l_len: off_t,
    val l_pid: pid_t,
) {
    public val whence: Whence
        get() = requireNotNull(Whence.fromIdOrNull(l_whence.toInt())) {
            "Unknown whence $l_whence"
        }

    public companion object {
        public const val STRUCT_FLOCK_SIZE: Int = 32

        public fun unpack(
            source: Source,
        ): StructFlock {
            source.require(STRUCT_FLOCK_SIZE.toLong())
            val type = source.readShortLe() // 0
            val whence = source.readShortLe() // 2
            source.readIntLe() // 4, padding?
            val start = source.readLongLe() // 8
            val len = source.readLongLe() // 16
            val pid = source.readIntLe() // 24

            return StructFlock(
                l_type = type,
                l_whence = whence,
                l_start = start.toULong(),
                l_len = len.toULong(),
                l_pid = pid,
            )
        }
    }
}
