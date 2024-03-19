/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.getenv
import platform.posix.usleep

@OptIn(ExperimentalForeignApi::class)
internal actual fun getSystemProp(name: String, defaultValue: String): String {
    return getenv(name.uppercase().replace(".", "_"))?.toString() ?: defaultValue
}

internal actual fun yieldSleepAroundMSec() {
    usleep(1000U)
}
