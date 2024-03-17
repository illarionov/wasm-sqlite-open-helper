/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public class SysException(
    public val errNo: Errno,
    msg: String? = null,
    cause: Throwable? = null,
) : RuntimeException(msg, cause)
