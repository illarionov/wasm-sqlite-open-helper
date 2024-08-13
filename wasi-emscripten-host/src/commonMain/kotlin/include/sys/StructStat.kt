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
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileModeType
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTimespec
import ru.pixnews.wasm.sqlite.open.helper.host.include.blkcnt_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.blksize_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.dev_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.gid_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.ino_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.nlink_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.off_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.timeMillis
import ru.pixnews.wasm.sqlite.open.helper.host.include.uid_t

/**
 * <sys/stat.h> struct stat
 *
 * @param st_dev ID of device containing file
 * @param st_ino Inode number
 * @param st_mode File type and mode
 * @param st_nlink Number of hard links
 * @param st_uid User ID of owner
 * @param st_gid Group ID of owner
 * @param st_rdev Device ID (if special file)
 * @param st_size Total size, in bytes
 * @param st_blksize Block size for filesystem I/O
 * @param st_blocks Number of 512 B blocks allocated
 * @param st_atim Time of last access
 * @param st_mtim Time of last modification
 * @param st_ctim Time of last status change
 */
@Suppress("PropertyName")
public data class StructStat(
    val st_dev: dev_t,
    val st_ino: ino_t,
    val st_mode: FileModeType,
    val st_nlink: nlink_t,
    val st_uid: uid_t,
    val st_gid: gid_t,
    val st_rdev: dev_t,
    val st_size: off_t,
    val st_blksize: blksize_t,
    val st_blocks: blkcnt_t,
    val st_atim: StructTimespec,
    val st_mtim: StructTimespec,
    val st_ctim: StructTimespec,
) {
    override fun toString(): String {
        return "StructStat($st_dev/$st_rdev $st_ino $st_nlink; $st_mode $st_uid $st_gid; " +
                "$st_size $st_blksize $st_blocks; " +
                "${st_atim.tv_sec}:${st_atim.tv_nsec} " +
                "${st_mtim.tv_sec}:${st_mtim.tv_nsec} " +
                "${st_ctim.tv_sec}:${st_ctim.tv_nsec}" +
                ")"
    }
}

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
