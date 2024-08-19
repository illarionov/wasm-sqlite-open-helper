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
}
