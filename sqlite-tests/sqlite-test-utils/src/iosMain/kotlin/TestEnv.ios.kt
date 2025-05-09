/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.test.utils

/**
 * Workaround for https://youtrack.jetbrains.com/issue/KT-69709/
 */
public actual fun setupInputStreamBuffering() {
    // Not verified, assume it is not required
}

/**
 * Workaround for https://youtrack.jetbrains.com/issue/KT-69709/
 */
public actual fun flushBuffers() {
    // Not verified, assume it is not required
}
