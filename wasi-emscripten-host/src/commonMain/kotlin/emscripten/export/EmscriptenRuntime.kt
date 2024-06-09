/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export

import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadExports
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStackExports

public class EmscriptenRuntime private constructor(
    public val mainExports: EmscriptenMainExports,
    public val stackExports: EmscriptenStackExports,
    public val pthreadExports: EmscriptenPthreadExports?,
) {
    public val stack: EmscriptenStack = EmscriptenStack(stackExports)
    public val isMultiThread: Boolean get() = pthreadExports != null

    public fun initRuntime(memory: Memory): Unit = if (isMultiThread) {
        initMultithreadRuntime(memory)
    } else {
        initSingleThreadedRuntime(memory)
    }

    private fun initSingleThreadedRuntime(memory: Memory) {
        stack.stackCheckInit(memory)
        mainExports.__wasm_call_ctors.executeVoid()
    }

    private fun initMultithreadRuntime(memory: Memory) {
        stack.stackCheckInit(memory)
        mainExports.__wasm_call_ctors.executeVoid()
    }

    public companion object {
        public fun emscriptenSingleThreadedRuntime(
            mainExports: EmscriptenMainExports,
            stackExports: EmscriptenStackExports,
        ): EmscriptenRuntime = EmscriptenRuntime(mainExports, stackExports, null)

        public fun emscriptenMultithreadedRuntime(
            mainExports: EmscriptenMainExports,
            stackExports: EmscriptenStackExports,
            pthreadExports: EmscriptenPthreadExports,
        ): EmscriptenRuntime = EmscriptenRuntime(mainExports, stackExports, pthreadExports)
    }
}
