/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.test.utils.assertions

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.io.files.Path
import platform.posix.errno
import platform.posix.stat
import kotlin.test.fail

internal actual fun Path.getFileMode(): UInt {
    val absolutePath = this.toString()
    return memScoped {
        val statBuf: stat = alloc<stat>()
        val resultCode = stat(absolutePath, statBuf.ptr)
        if (resultCode != 0) {
            fail("Can not stat `$absolutePath`: error $errno")
        }
        statBuf.st_mode
    }
}
