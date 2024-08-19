/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Pages
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.EmscriptenResizeHeapFunctionHandle.Companion.calculateNewSizePages
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno

internal class EmscriptenResizeHeap(host: EmbedderHost) : EmscriptenHostFunctionHandle {
    private val logger: Logger = host.rootLogger.withTag("wasm-func:emscripten_resize_heap")

    override fun apply(instance: Instance, vararg args: Value): Value {
        val memory = instance.memory()
        val requestedSize = args[0].asInt().toLong()

        val newSizePages = calculateNewSizePages(
            requestedSize,
            Pages(memory.pages().toLong()),
            Pages(memory.maximumPages().toLong()),
        )

        logger.v {
            "emscripten_resize_heap($requestedSize). " +
                    "Requested: ${newSizePages.inBytes} bytes ($newSizePages pages)"
        }

        val prevPages = memory.grow((newSizePages.count - memory.pages()).toInt())
        if (prevPages < 0) {
            logger.e {
                "Cannot enlarge memory, requested $newSizePages pages, but the limit is " +
                        "${memory.maximumPages()} pages!"
            }
            return Value.i32(-Errno.NOMEM.code.toLong())
        }
        return Value.i32(1)
    }
}
