/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory.PAGE_SIZE
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.EmscriptenResizeHeapFunctionHandle.Companion.calculateNewSizePages
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

internal class EmscriptenResizeHeap(
    host: SqliteEmbedderHost,
    private val memory: Memory,
) : EmscriptenHostFunctionHandle {
    private val logger: Logger = host.rootLogger.withTag("wasm-func:emscripten_resize_heap")

    override fun apply(instance: Instance, vararg args: Value): Value {
        val memory = instance.memory()
        val requestedSize = args[0].asInt().toLong()

        val newSizePages = calculateNewSizePages(
            requestedSize,
            memory.pages().toLong(),
            memory.maximumPages().toLong(),
        )

        logger.v {
            "emscripten_resize_heap($requestedSize). " +
                    "Requested: ${newSizePages * PAGE_SIZE} bytes ($newSizePages pages)"
        }

        val prevPages = memory.grow((newSizePages - memory.pages()).toInt())
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
