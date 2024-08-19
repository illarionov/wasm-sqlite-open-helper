/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.SUCCESS
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.SCHED_YIELD

public class SchedYieldFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(SCHED_YIELD, host) {
    public fun execute(): Errno {
        Thread.yield()
        return SUCCESS
    }
}
