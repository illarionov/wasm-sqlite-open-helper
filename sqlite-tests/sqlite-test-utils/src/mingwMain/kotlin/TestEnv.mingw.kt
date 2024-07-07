/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.test.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix._IONBF
import platform.posix.setvbuf
import platform.posix.stderr
import platform.posix.stdout

/**
 * Workaround for https://youtrack.jetbrains.com/issue/KT-69709/
 */
@OptIn(ExperimentalForeignApi::class)
public actual fun setupInputStreamBuffering() {
    listOf(stdout, stderr).forEach {
        check(setvbuf(it, null, _IONBF, 0U) == 0)
    }
}
