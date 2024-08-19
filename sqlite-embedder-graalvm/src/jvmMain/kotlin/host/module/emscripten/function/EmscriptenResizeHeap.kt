/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenResizeHeap.ResizeHeapHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Pages
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.EmscriptenResizeHeapFunctionHandle.Companion.calculateNewSizePages
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.NOMEM

internal class EmscriptenResizeHeap(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
) : BaseWasmNode<ResizeHeapHandle>(language, module, ResizeHeapHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        return handle.execute(
            memory(frame),
            frame.arguments.getArgAsInt(0).toLong(),
        )
    }

    internal class ResizeHeapHandle(
        host: EmbedderHost,
    ) : HostFunctionHandle(EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP, host) {
        @TruffleBoundary
        fun execute(
            memory: WasmMemory,
            requestedSize: Long,
        ): Int {
            val currentPages = Pages(memory.size())
            val declaredMaxPages = Pages(memory.declaredMaxSize())
            val newSizePages = calculateNewSizePages(requestedSize, currentPages, declaredMaxPages)

            logger.v {
                "emscripten_resize_heap($requestedSize). " +
                        "Requested: ${newSizePages.inBytes} bytes ($newSizePages pages)"
            }

            val memoryAdded = memory.grow(newSizePages.count - currentPages.count)
            return if (!memoryAdded) {
                return -NOMEM.code
            } else {
                1
            }
        }
    }
}
