/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.threadfactory

import at.released.weh.host.base.POINTER
import at.released.weh.wasm.core.HostFunction

internal val EXTERNAL_MANAGED_THREAD_START_ROUTINE = object : HostFunction {
    override val wasmName: String = "use_managed_thread_pthread_routine"
    override val type: HostFunction.HostFunctionType = HostFunction.HostFunctionType(
        params = listOf(POINTER),
        returnTypes = listOf(POINTER),
    )
}
