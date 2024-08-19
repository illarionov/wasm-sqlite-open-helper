/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber", "ConstructorParameterNaming", "TYPEALIAS_NAME_INCORRECT_CASE")

package ru.pixnews.wasm.sqlite.open.helper.host.include.sys

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.writeIntLe
import kotlinx.io.writeLongLe
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.timeMillis

public fun StructStat.packTo(sink: Sink): Unit = sink.run {
    writeIntLe(st_dev.toInt()) // 0
    writeIntLe(st_mode.mask.toInt()) // 4
    writeIntLe(st_nlink.toInt()) // 8
    writeIntLe(st_uid.toInt()) // 12
    writeIntLe(st_gid.toInt()) // 16
    writeIntLe(st_rdev.toInt()) // 20
    writeLongLe(st_size.toLong()) // 24
    writeIntLe(4096) // 32
    writeIntLe(st_blocks.toInt()) // 36

    st_atim.timeMillis.let {
        writeLongLe((it / 1000U).toLong()) // 40
        writeIntLe((1000U * (it % 1000U)).toInt()) // 48
        writeIntLe(0) // 52, padding
    }
    st_mtim.timeMillis.let {
        writeLongLe((it / 1000U).toLong()) // 56
        writeIntLe((1000U * (it % 1000U)).toInt()) // 64
        writeIntLe(0) // 68, padding
    }
    st_ctim.timeMillis.let {
        writeLongLe((it / 1000U).toLong()) // 72
        writeIntLe((1000U * (it % 1000U)).toInt()) // 80
        writeIntLe(0) // 84, padding
    }
    writeLongLe(st_ino.toLong()) // 88
}

public const val STRUCT_SIZE_PACKED_SIZE: Int = 96

public fun StructStat.pack(): Buffer = Buffer().also {
    packTo(it)
    check(it.size == STRUCT_SIZE_PACKED_SIZE.toLong())
}
