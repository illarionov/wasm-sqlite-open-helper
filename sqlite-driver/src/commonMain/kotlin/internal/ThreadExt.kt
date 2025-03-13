/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.internal

import at.released.weh.common.api.Logger

internal expect val currentThreadId: ULong

internal fun checkCurrentThread(logger: Logger, expected: ULong) {
    if (currentThreadId != expected) {
        logger.w {
        "This function must be called on the same thread in which the SQLite connection is open." +
                "Thread: $currentThreadId, expected: $expected"
        }
    }
}
