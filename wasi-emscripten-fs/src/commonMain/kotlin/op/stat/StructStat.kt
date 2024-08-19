/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat

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
@Suppress("PropertyName", "ConstructorParameterNaming")
public data class StructStat(
    val st_dev: ULong,
    val st_ino: ULong,
    val st_mode: FileModeType,
    val st_nlink: ULong,
    val st_uid: ULong,
    val st_gid: ULong,
    val st_rdev: ULong,
    val st_size: ULong,
    val st_blksize: ULong,
    val st_blocks: ULong,
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
