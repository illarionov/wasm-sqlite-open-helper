/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.binding.IndirectFunctionBindingProvider
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthread
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadInternal
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread

internal class ManagedPthread(
    name: String,
    override var pthreadPtr: WasmPtr<StructPthread>?,
    private val startRoutine: Int,
    private val arg: WasmPtr<Unit>,
    stateListener: StateListener,
    emscriptenPthread: EmscriptenPthread,
    emscriptenPthreadInternal: EmscriptenPthreadInternal,
    threadInitializer: ManagedThreadInitializer,
    private val indirectBindingProvider: IndirectFunctionBindingProvider,
) : ManagedThreadBase(
    name = name,
    emscriptenPthread = emscriptenPthread,
    pthreadInternal = emscriptenPthreadInternal,
    threadInitializer = threadInitializer,
    stateListener = stateListener,
) {
    override fun managedRun() {
        invokeStartRoutine()
    }

    private fun invokeStartRoutine() {
        indirectBindingProvider.getFunctionBinding(
            IndirectFunctionTableIndex(startRoutine),
        )?.executeForInt(arg.addr) ?: error("Indirect function `$startRoutine` not registered")
    }
}
