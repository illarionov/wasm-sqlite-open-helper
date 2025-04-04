/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.test.utils

public object TestEnv {
    public fun prepareTestEnvBeforeTest() {
        setupInputStreamBuffering()
    }

    public fun afterTest() {
        flushBuffers()
    }
}

/**
 * Workaround for https://youtrack.jetbrains.com/issue/KT-69709/
 */
public expect fun setupInputStreamBuffering()

/**
 * Workaround for https://youtrack.jetbrains.com/issue/KT-69709/
 */
public expect fun flushBuffers()
