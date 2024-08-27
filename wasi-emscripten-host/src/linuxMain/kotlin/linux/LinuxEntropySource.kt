/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.linux

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.linux.SYS_getrandom
import platform.posix.errno
import platform.posix.syscall
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.EntropySource

internal object LinuxEntropySource : EntropySource {
    override fun generateEntropy(size: Int): ByteArray {
        val bytes = ByteArray(size)
        var bytesLeft: Int = size
        bytes.usePinned {
            while (bytesLeft != 0) {
                val startPosition = size - bytesLeft
                val bytesWritten = syscall(SYS_getrandom.toLong(), it.addressOf(startPosition), bytesLeft.toULong(), 0U)
                if (bytesWritten < 0) {
                    error("Can not generate entropy. Errno: $errno")
                }
                bytesLeft -= bytesWritten.toInt()
            }
        }
        return bytes
    }
}
