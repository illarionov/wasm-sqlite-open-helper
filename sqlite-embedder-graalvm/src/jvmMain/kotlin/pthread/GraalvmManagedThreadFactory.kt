/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class GraalvmManagedThreadFactory : ThreadFactory {
    private val threadNumber = AtomicInteger(1)

    override fun newThread(runnable: Runnable): Thread {
        val name = "graalvm-embedder-thread-${threadNumber.getAndDecrement()}"
        val thread = Thread(null, runnable, name)
        if (thread.isDaemon) {
            thread.isDaemon = false
        }

        return thread
    }
}
