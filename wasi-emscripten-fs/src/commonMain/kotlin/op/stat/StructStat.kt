/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat

/**
 * <sys/stat.h> struct stat
 *
 * @param deviceId ID of device containing file
 * @param inode Inode number
 * @param mode File type and mode
 * @param links Number of hard links
 * @param usedId User ID of owner
 * @param groupId Group ID of owner
 * @param specialFileDeviceId Device ID (if special file)
 * @param size Total size, in bytes
 * @param blockSize Block size for filesystem I/O
 * @param blocks Number of 512 B blocks allocated
 * @param accessTime Time of last access
 * @param modificationTime Time of last modification
 * @param changeStatusTime Time of last status change
 */
public data class StructStat(
    val deviceId: ULong,
    val inode: ULong,
    val mode: FileModeType,
    val links: ULong,
    val usedId: ULong,
    val groupId: ULong,
    val specialFileDeviceId: ULong,
    val size: ULong,
    val blockSize: ULong,
    val blocks: ULong,
    val accessTime: StructTimespec,
    val modificationTime: StructTimespec,
    val changeStatusTime: StructTimespec,
) {
    override fun toString(): String {
        return "StructStat($deviceId/$specialFileDeviceId $inode $links; $mode $usedId $groupId; " +
                "$size $blockSize $blocks; " +
                "${accessTime.seconds}:${accessTime.nanoseconds} " +
                "${modificationTime.seconds}:${modificationTime.nanoseconds} " +
                "${changeStatusTime.seconds}:${changeStatusTime.nanoseconds}" +
                ")"
    }
}
