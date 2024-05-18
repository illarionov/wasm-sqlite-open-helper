/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd

import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence

internal fun FdFileChannel.resolveWhencePosition(
    offset: Long,
    whence: Whence,
): Long = when (whence) {
    Whence.SET -> offset
    Whence.CUR -> position + offset
    Whence.END -> channel.size() - offset
}
