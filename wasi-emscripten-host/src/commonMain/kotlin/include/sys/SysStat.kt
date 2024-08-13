/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include.sys

/**
 * Constants from Emscripten include/sys/stat.h
 */
@Suppress("NoMultipleSpaces", "TOO_MANY_CONSECUTIVE_SPACES", "BLANK_LINE_BETWEEN_PROPERTIES")
public object SysStat {
    public const val UTIME_NOW: UInt = 0x3fff_ffff_U
    public const val UTIME_OMIT: UInt = 0x3fff_fffe_U

    public const val S_IFMT:   UInt = 0b000_001_111_000_000_000_000U
    public const val S_IFDIR:  UInt = 0b000_000_100_000_000_000_000U
    public const val S_IFCHR:  UInt = 0b000_000_010_000_000_000_000U
    public const val S_IFBLK:  UInt = 0b000_000_110_000_000_000_000U
    public const val S_IFREG:  UInt = 0b000_001_000_000_000_000_000U
    public const val S_IFIFO:  UInt = 0b000_000_001_000_000_000_000U
    public const val S_IFLNK:  UInt = 0b000_001_010_000_000_000_000U
    public const val S_IFSOCK: UInt = 0b000_001_100_000_000_000_000U

    public const val S_ISUID: UInt = 0b100_000_000_000U
    public const val S_ISGID: UInt = 0b010_000_000_000U
    public const val S_ISVTX: UInt = 0b001_000_000_000U
    public const val S_IRUSR: UInt = 0b000_100_000_000U
    public const val S_IWUSR: UInt = 0b000_010_000_000U
    public const val S_IXUSR: UInt = 0b000_001_000_000U
    public const val S_IRWXU: UInt = 0b000_111_000_000U
    public const val S_IRGRP: UInt = 0b000_000_100_000U
    public const val S_IWGRP: UInt = 0b000_000_010_000U
    public const val S_IXGRP: UInt = 0b000_000_001_000U
    public const val S_IRWXG: UInt = 0b000_000_111_000U
    public const val S_IROTH: UInt = 0b000_000_000_100U
    public const val S_IWOTH: UInt = 0b000_000_000_010U
    public const val S_IXOTH: UInt = 0b000_000_000_001U
    public const val S_IRWXO: UInt = 0b000_000_000_111U
}
