/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber", "ConstructorParameterNaming", "TYPEALIAS_NAME_INCORRECT_CASE")

package ru.pixnews.wasm.sqlite.open.helper.host.include.sys

import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTimespec
import ru.pixnews.wasm.sqlite.open.helper.host.include.timeMillis
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    val st_mode: FileMode,
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
)

public typealias blkcnt_t = ULong
public typealias blksize_t = ULong
public typealias dev_t = ULong
public typealias gid_t = ULong
public typealias ino_t = ULong
public typealias nlink_t = ULong
public typealias off_t = ULong
public typealias uid_t = ULong

public fun StructStat.pack(): ByteArray {
    val dstByteArray = ByteArray(96)
    val dst = ByteBuffer.wrap(dstByteArray).apply {
        order(ByteOrder.LITTLE_ENDIAN)
    }

    dst.putInt(0, st_dev.toInt())
    dst.putInt(4, st_mode.mask.toInt())
    dst.putInt(8, st_nlink.toInt())
    dst.putInt(12, st_uid.toInt())
    dst.putInt(16, st_gid.toInt())
    dst.putInt(20, st_rdev.toInt())
    dst.putLong(24, st_size.toLong())
    dst.putInt(32, 4096)
    dst.putInt(36, st_blocks.toInt())

    st_atim.timeMillis.let {
        dst.putLong(40, (it / 1000U).toLong())
        dst.putInt(48, (1000U * (it % 1000U)).toInt())
    }
    st_mtim.timeMillis.let {
        dst.putLong(56, (it / 1000U).toLong())
        dst.putInt(64, (1000U * (it % 1000U)).toInt())
    }
    st_ctim.timeMillis.let {
        dst.putLong(72, (it / 1000U).toLong())
        dst.putInt(80, (1000U * (it % 1000U)).toInt())
    }
    dst.putLong(88, st_ino.toLong())

    return dstByteArray
}
